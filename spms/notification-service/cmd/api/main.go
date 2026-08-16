package main

import (
	"log"

	"notification-service/internal/config"
	"notification-service/internal/consumer"
	"notification-service/internal/handler"
	"notification-service/internal/server"
)

func main() {
	// 1. Load configuration
	cfg := config.LoadConfig()
	log.Printf("[BOOT]  Notification Service starting — port=%s, exchange=%s, queue=%s",
		cfg.Port, cfg.RabbitMQExchange, cfg.RabbitMQQueue)

	// 2. Initialize shared stats (thread-safe atomic counters)
	stats := &handler.Stats{}

	// 3. Start RabbitMQ consumer in a background goroutine
	//       It blocks internally, retrying on connection failure.
	go func() {
		c := consumer.New(cfg.RabbitMQURL, cfg.RabbitMQExchange, cfg.RabbitMQQueue, stats)
		c.Start() // never returns under normal operation
	}()

	// 3. Start Gin HTTP server (blocks)
	srv := server.New(cfg.Port, stats)
	log.Printf("[BOOT]  HTTP server listening on :%s", cfg.Port)
	if err := srv.Run(); err != nil {
		log.Fatalf("[FATAL] HTTP server failed: %v", err)
	}
}
