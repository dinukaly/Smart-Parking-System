package server

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"notification-service/internal/handler"
)

// Server wraps the Gin HTTP engine for the Notification Service.
type Server struct {
	router *gin.Engine
	stats  *handler.Stats
	port   string
}

// New creates a Gin HTTP server with all routes registered.
func New(port string, stats *handler.Stats) *Server {
	s := &Server{
		router: gin.Default(),
		stats:  stats,
		port:   port,
	}
	s.registerRoutes()
	return s
}

// Run starts listening for HTTP requests. Blocks the caller.
func (s *Server) Run() error {
	return s.router.Run(":" + s.port)
}

// Route Registration

func (s *Server) registerRoutes() {
	s.router.GET("/health", s.health)
	s.router.GET("/status", s.status)
}

// Handlers

// health godoc
// GET /health
// Returns a simple OK response to confirm the service is running.
func (s *Server) health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status":  "ok",
		"service": "notification-service",
		"version": "1.0.0",
	})
}

// status godoc
// GET /status
// Returns event processing statistics gathered since service startup.
func (s *Server) status(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"status":  "ok",
		"service": "notification-service",
		"metrics": s.stats.Snapshot(),
	})
}
