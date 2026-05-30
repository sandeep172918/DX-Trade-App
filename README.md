# DX-Trade - Real-Time Stock Trading Backend

DX-Trade is a high-performance, headless stock trading simulation backend. It features an in-memory matching engine, real-time market data integration with Yahoo Finance, and a full set of REST and WebSocket APIs for trading.

## Tech Stack

- **Java 21 / Spring Boot 3+**
- **Spring Security (JWT)**: Stateless authentication.
- **Spring Data JPA / PostgreSQL**: Persistent storage for users, orders, trades, holdings, and transactions.
- **Spring WebSocket (STOMP)**: Real-time broadcasting of high-frequency price ticks.
- **Redis**: Used for pub/sub and high-speed caching.
- **In-Memory Matching Engine**: FIFO (Price-Time Priority) logic using concurrent data structures.

## Core Features
1. **Real-Time Market Data**: Autonomously fetches real-time stock prices from Yahoo Finance API every 5 seconds for active stocks and broadcasts updates via WebSocket.
2. **Order Matching Engine**: Realistic matching of BUY and SELL orders (Limit/Market).
3. **Secure Wallet System**: Fund management with:
   - MPIN-secured simulated deposit engine (default MPIN is `0000`).
   - Deposit size constraints (between $0.01 and $5,000 per transaction).
   - Balance limitation (up to $100,000 max balance).
   - Transaction logging with unique reference IDs.
4. **Portfolio Tracking**: Real-time P&L and holdings management.
5. **Interactive CLI Client**: Full-featured interactive CLI utility for simulation, trading, depositing, and market watching.

## Getting Started

### Prerequisites
- Docker & Docker Compose
- Java 21

### Running the Application

To run the full stack (database, redis, and backend) via Docker:
```bash
docker-compose up --build -d
```
When running with Docker:
* Backend is hosted on `http://localhost:8090`
* PostgreSQL runs on `5432`
* Redis runs on `6379`

Or run the services locally (make sure PostgreSQL on port `5433` and Redis on port `6380` are running):
```bash
./mvnw spring-boot:run
```
The backend API will be available at `http://localhost:8090`.

### Interactive CLI Client
You can run the interactive scripts directly to simulate a client:
```bash
./test_flow.sh
```
This utility lets you register, log in, view wallet & transaction histories, simulate cash deposits, view stock prices, place or cancel orders, track portfolio holdings, and modify your MPIN.

## API Documentation

### Authentication
- `POST /api/auth/register`: Register a new user.
- `POST /api/auth/login`: Login and receive a JWT token.

### Stocks & Market Data
- `GET /api/public/stocks`: List all active stocks with current prices.
- `GET /api/public/stocks/{symbol}/quote`: Get a real-time synthetic quote.
- `GET /api/public/stocks/{symbol}/candles`: Get synthetic OHLCV history.

### Wallet Management
- `GET /api/wallet`: Get the current user's wallet details.
- `GET /api/wallet/transactions`: Fetch transaction history logs.
- `POST /api/wallet/simulate-deposit`: Add simulated cash (requires secure token/headers and the user's 4-digit MPIN).
- `POST /api/wallet/change-mpin`: Change user's 4-digit transaction MPIN.

### Real-Time Updates (WebSocket)
Connect via STOMP to `ws://localhost:8090/ws` and subscribe to:
- `/topic/stocks`: Real-time Yahoo Finance price ticks (pushed every 5 seconds).
- `/user/queue/orders`: Personal order status updates.
