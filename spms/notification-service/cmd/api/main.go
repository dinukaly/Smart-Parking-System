package main

import (
	"log"

	"github.com/gin-gonic/gin"
	"notification-service/internal/config"
)

func main() {
	cfg := config.LoadConfig()

	gin.SetMode(cfg.GinMode)
	router := gin.Default()

	// Scaffolding Health Check
	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status":  "ok",
			"service": "notification-service",
		})
	})

	log.Printf("Notification Service starting on port %s...", cfg.Port)
	if err := router.Run(":" + cfg.Port); err != nil {
		log.Fatalf("Failed to run server: %v", err)
	}
}
