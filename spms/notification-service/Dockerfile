# Build Stage
FROM golang:1.21-alpine AS builder

WORKDIR /app

# Install git and ca-certificates
RUN apk update && apk add --no-cache git ca-certificates

# Copy dependency definitions
COPY go.mod go.sum* ./
RUN go mod download || true

# Copy source code
COPY . .

# Build statically linked binary
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -ldflags="-w -s" -o notification-service ./cmd/api/main.go

# Production Stage
FROM alpine:3.19

WORKDIR /app

RUN apk --no-cache add ca-certificates tzdata

COPY --from=builder /app/notification-service .

EXPOSE 8085

CMD ["./notification-service"]
