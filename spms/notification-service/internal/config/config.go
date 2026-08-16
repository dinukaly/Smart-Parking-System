package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

// Config holds all configuration properties for Notification Service
type Config struct {
	Port             string
	GinMode          string
	RabbitMQURL      string
	RabbitMQExchange string
	RabbitMQQueue    string
}

// LoadConfig loads configuration from environment variables or .env file
func LoadConfig() *Config {
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found or error loading, reading from environment variables")
	}

	return &Config{
		Port:             getEnv("PORT", "8085"),
		GinMode:          getEnv("GIN_MODE", "release"),
		RabbitMQURL:      getEnv("RABBITMQ_URL", "amqp://spms_admin:spms_password@localhost:5672/"),
		RabbitMQExchange: getEnv("RABBITMQ_EXCHANGE", "spms.events"),
		RabbitMQQueue:    getEnv("RABBITMQ_QUEUE", "notification.queue"),
	}
}

func getEnv(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists && value != "" {
		return value
	}
	return fallback
}
