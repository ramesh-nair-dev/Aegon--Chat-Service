# WhatsApp-Like Distributed Chat System

A production-grade, one-to-one chat system built step-by-step to understand **real-world messaging systems**, focusing on correctness first and scaling only when pain appears.

This project evolves from a single-server WebSocket prototype into a horizontally scalable, ACK-driven distributed system with strong delivery guarantees.

---

## 🚀 What This Project Demonstrates

* Real-time messaging using WebSockets
* Durable message storage with MySQL (source of truth)
* Offline message replay
* Multi-device user presence
* Horizontal scaling across multiple servers
* Client-verified delivery and read guarantees
* Crash-safe, idempotent message handling

This project prioritizes **engineering judgment over feature count**.

---

## 🧠 Core Design Principles

* Never scale before pain appears
* Database is the source of truth
* Servers are stateless with respect to users
* Presence is soft state (TTL-based)
* Servers attempt delivery, clients confirm it
* Correctness > throughput > convenience

---

## 🏗️ High-Level Architecture

* **WebSockets** for real-time communication
* **MySQL** for durable message storage
* **Redis** for:

  * Presence tracking (TTL-based)
  * Cross-server routing
* **Redis Pub/Sub** for server-to-server message delivery
* **ACK-driven state machine** for delivery correctness

---

## 📬 Message Lifecycle

```
CREATED   → stored in database
SENT      → sender acknowledged
DELIVERED → client acknowledged receipt
READ      → client acknowledged read
```

Key rule:
**Only the client is allowed to assert DELIVERED and READ states.**

---

## 📦 Implemented Phases

### Phase 1 — Real-Time Messaging

* WebSocket-based one-to-one chat
* In-memory session management

### Phase 2 — Durability

* Messages stored in MySQL before delivery
* Server crashes no longer lose messages

### Phase 3 — Offline Messaging

* Undelivered messages replay on reconnect

### Phase 4 — Presence & Multi-Device Support

* Redis-based presence with TTL
* User considered online if any device is online

### Phase 5 — Horizontal Scaling

* Stateless servers
* Redis Pub/Sub for cross-server routing
* Users can connect to any server

### Phase 6 — Delivery Guarantees

* Client-verified DELIVERY_ACK and READ_ACK
* At-least-once delivery semantics
* Idempotent, crash-safe state transitions

---

## 📊 Capacity (Realistic & Defensible)

* ~50,000 concurrent users per server
* ~500,000 concurrent users with horizontal scaling (10 servers)

Assumes normal chat behavior and correctness-first operation.

---

## 🧪 Failure Handling

* Server crashes → messages replay safely
* Client crashes → delivery not falsely acknowledged
* Redis failure → correctness preserved via DB replay
* Duplicate ACKs → safely ignored (idempotent)

---

## 🛠️ Tech Stack

* Java / Spring Boot
* WebSockets
* MySQL
* Redis
* Redis Pub/Sub

---

## 🎯 Why This Project Exists

This is not a WhatsApp clone.

It is a **learning-driven system design project** built to deeply understand:

* Message truth
* Delivery guarantees
* Distributed coordination
* Failure modes in real systems

---

## 📌 Status

✔ Complete up to **Phase 6**
⬛ Phase 7 (Production hardening & observability) — conceptual

---

## 🧠 Key Takeaway

> A chat system isn’t about sending messages.
> It’s about deciding who is allowed to say what actually happened.

---

## 👤 Author

**Ramesh Nair**

* Backend Engineer | Java | Spring Boot | System Design Enthusiast
* Focused on building **scalable, maintainable, real-world systems**.
* Passionate about **clean architecture, design patterns, and domain modeling**.

📫 Reach me at: ramesh200212@gmail.com
🌐 GitHub: https://github.com/ramesh-nair-dev

---
