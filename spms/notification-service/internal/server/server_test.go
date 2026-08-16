package server

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"notification-service/internal/handler"
)

func init() {
	gin.SetMode(gin.TestMode)
}

func TestHealthEndpoint(t *testing.T) {
	stats := &handler.Stats{}
	srv := New("8085", stats)

	req, _ := http.NewRequest(http.MethodGet, "/health", nil)
	w := httptest.NewRecorder()

	srv.router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("Expected status code %d, got %d", http.StatusOK, w.Code)
	}

	var resp map[string]interface{}
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}

	if resp["status"] != "ok" {
		t.Errorf("Expected status 'ok', got '%v'", resp["status"])
	}
	if resp["service"] != "notification-service" {
		t.Errorf("Expected service 'notification-service', got '%v'", resp["service"])
	}
}

func TestStatusEndpoint(t *testing.T) {
	stats := &handler.Stats{}
	stats.ParkingReserved.Add(3)
	stats.PaymentSuccess.Add(2)
	stats.TotalProcessed.Add(5)

	srv := New("8085", stats)

	req, _ := http.NewRequest(http.MethodGet, "/status", nil)
	w := httptest.NewRecorder()

	srv.router.ServeHTTP(w, req)

	if w.Code != http.StatusOK {
		t.Fatalf("Expected status code %d, got %d", http.StatusOK, w.Code)
	}

	var resp map[string]interface{}
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("Failed to parse response: %v", err)
	}

	metrics, ok := resp["metrics"].(map[string]interface{})
	if !ok {
		t.Fatalf("Expected metrics map in response, got %v", resp["metrics"])
	}

	if metrics["parking_reserved"] != float64(3) {
		t.Errorf("Expected parking_reserved = 3, got %v", metrics["parking_reserved"])
	}
	if metrics["payment_success"] != float64(2) {
		t.Errorf("Expected payment_success = 2, got %v", metrics["payment_success"])
	}
	if metrics["total_processed"] != float64(5) {
		t.Errorf("Expected total_processed = 5, got %v", metrics["total_processed"])
	}
}
