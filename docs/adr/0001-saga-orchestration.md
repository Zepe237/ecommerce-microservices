# ADR 0001 — Saga orchestrée par l'order-service pour le paiement

- **Statut** : accepté
- **Date** : 2026-11-20

## Contexte

Passer une commande touche quatre services, chacun avec sa propre base de données :
order (la commande), inventory (le stock), payment (le paiement), cart et notification (les effets de bord).
Une transaction distribuée (2PC) n'est pas envisageable : Kafka et les bases PostgreSQL séparées ne la supportent
pas proprement, et elle couplerait la disponibilité de tous les services.

Le flux précédent avait deux défauts :

1. le stock était **vérifié** puis **décrémenté** plus tard, sur `order.created` : entre les deux,
   deux clients pouvaient acheter la dernière unité ;
2. la commande était `CONFIRMED` **avant** le paiement : un paiement refusé laissait une commande confirmée
   et un stock décrémenté.

## Décision

Une **saga orchestrée** dont l'order-service est le chef d'orchestre. Chaque étape est une transaction locale,
et chaque échec déclenche une compensation.

| # | Étape | Mécanisme | En cas d'échec |
|---|---|---|---|
| 1 | La commande est enregistrée `PAYMENT_PENDING` | transaction locale | — |
| 2 | Le stock est **réservé**, tout ou rien | appel HTTP synchrone (Feign) | 409 → commande `CANCELLED`, le client reçoit 409 |
| 3 | Le client paie | `POST /api/payments` | — |
| 4 | Le résultat est publié | `payment.completed` (Kafka) | — |
| 5 | La commande est décidée | `CONFIRMED` ou `CANCELLED`, publiée **après commit** sur `order.confirmed` / `order.cancelled` | — |
| 6 | Le stock est **validé** ou **libéré** | inventory écoute les deux topics | — |
| 7 | Le panier est vidé, l'e-mail envoyé | cart et notification écoutent `order.confirmed` | un paiement refusé garde le panier |
| 8 | Une commande non payée expire | balayage `@Scheduled`, 15 min | `order.cancelled` → stock libéré |

Choix de mise en œuvre :

- **Réservation synchrone.** Le client doit savoir tout de suite s'il manque du stock. Un événement
  ne permettrait pas de lui répondre 409.
- **Décision asynchrone.** Trois services réagissent à la décision : un événement suffit, sans que
  l'order-service ait à les connaître ni à attendre qu'ils soient en ligne.
- **Réservation atomique.** Chaque réservation est un `UPDATE ... WHERE quantity - reserved >= :q`
  conditionnel : pas de lecture puis écriture, donc pas de survente. Les produits sont traités dans l'ordre
  de leur identifiant (`TreeMap`) pour éviter les interblocages entre deux commandes concurrentes.
- **Idempotence partout.** Kafka livre « au moins une fois ». Une commande déjà décidée ignore un second
  `payment.completed` ; `commit` et `release` ne traitent que les lignes encore `RESERVED` ; une notification
  déjà envoyée n'est pas renvoyée ; un paiement est unique par commande (contrainte d'unicité + 409).
- **Publication après commit.** L'événement de décision est envoyé dans `afterCommit()` : un rollback
  n'annonce rien de faux.

## Alternatives écartées

- **Chorégraphie** (chaque service réagit à l'événement du précédent, sans chef). Plus découplée, mais le
  déroulé de la commande n'est écrit nulle part : il faut lire quatre services pour le comprendre, et
  les compensations sont dispersées. L'orchestration garde la logique métier de la commande à un seul endroit.
- **Décrémenter le stock sur `order.created`** (l'ancien flux) : survente possible et pas de retour arrière.
- **Réserver le stock par événement** : le client recevrait 201 puis apprendrait plus tard qu'il manque un
  produit, sans canal pour le prévenir.

## Conséquences et limites connues

Positives :

- plus de survente ; un paiement refusé ou abandonné rend le stock ;
- le panier n'est perdu que si la commande est réellement confirmée.

Limites assumées, et la solution qu'on choisirait en production :

1. **Pas d'outbox.** Si Kafka est indisponible au moment de `afterCommit()`, la décision est en base mais
   l'événement est perdu (le stock reste réservé). → *Transactional outbox* : l'événement est écrit dans
   une table de la même transaction, puis relayé vers Kafka.
2. **Course paiement / expiration.** Si l'expiration annule la commande pendant que le prestataire débite
   le client, on obtient une commande `CANCELLED` payée. → Sur `payment.completed` `SUCCEEDED` pour une
   commande déjà annulée, déclencher un **remboursement** automatique.
3. **Pas de verrou optimiste sur `Order`.** Deux décisions simultanées (paiement et expiration) peuvent se
   croiser. → Ajouter `@Version` ou une mise à jour conditionnelle `WHERE status = 'PAYMENT_PENDING'`.
4. **Pas de dead-letter topic.** Un message qui échoue après ses relances est seulement journalisé.
5. **Pas d'e-mail en cas d'annulation.** Seule la confirmation est notifiée.
