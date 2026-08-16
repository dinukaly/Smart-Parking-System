package handler

import (
	"encoding/json"
	"fmt"
	"log"
	"sync/atomic"
)



// Stats — atomic counters per event type
// Exposed via GET /status

type Stats struct {
	ParkingReserved       atomic.Int64
	PaymentSuccess        atomic.Int64
	PaymentFailed         atomic.Int64
	PaymentRefunded       atomic.Int64
	ReservationCancelled  atomic.Int64
	VehicleEntered        atomic.Int64
	VehicleExited         atomic.Int64
	UnknownEvents         atomic.Int64
	TotalProcessed        atomic.Int64
}

// Snapshot returns a plain map safe for JSON marshalling
func (s *Stats) Snapshot() map[string]int64 {
	return map[string]int64{
		"parking_reserved":      s.ParkingReserved.Load(),
		"payment_success":       s.PaymentSuccess.Load(),
		"payment_failed":        s.PaymentFailed.Load(),
		"payment_refunded":      s.PaymentRefunded.Load(),
		"reservation_cancelled": s.ReservationCancelled.Load(),
		"vehicle_entered":       s.VehicleEntered.Load(),
		"vehicle_exited":        s.VehicleExited.Load(),
		"unknown_events":        s.UnknownEvents.Load(),
		"total_processed":       s.TotalProcessed.Load(),
	}
}

// Dispatcher — routes routing key → handler

// Dispatch routes an incoming AMQP message to the correct handler based on routingKey.
func Dispatch(routingKey string, body []byte, stats *Stats) {
	defer func() {
		if r := recover(); r != nil {
			log.Printf("[ERROR] Panic in handler for routingKey=%s: %v", routingKey, r)
		}
	}()

	stats.TotalProcessed.Add(1)

	switch routingKey {
	case "parking.reserved":
		handleParkingReserved(body, stats)

	case "payment.success", "payment.completed":
		handlePaymentSuccess(body, stats)

	case "payment.failed":
		handlePaymentFailed(body, stats)

	case "payment.refunded":
		handlePaymentRefunded(body, stats)

	case "reservation.cancelled":
		handleReservationCancelled(body, stats)

	case "vehicle.entered":
		handleVehicleEntered(body, stats)

	case "vehicle.exited":
		handleVehicleExited(body, stats)

	default:
		stats.UnknownEvents.Add(1)
		log.Printf("[WARN]  Unknown routing key received: %s", routingKey)
	}
}



// Individual Notification Handlers

func handleParkingReserved(body []byte, stats *Stats) {
	var evt ParkingReservedEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse parking.reserved event: %v | raw: %s", err, string(body))
		return
	}

	stats.ParkingReserved.Add(1)

	// Mock email: Booking Confirmation
	log.Printf("[EMAIL] ✉️  Booking Confirmation Sent")
	log.Printf("         To:      %s", evt.UserEmail)
	log.Printf("         Subject: Your Parking Reservation is Confirmed!")
	log.Printf("         Body:    Hi! Your reservation at [%s] is confirmed.", evt.ParkingSpaceName)
	log.Printf("                  Reservation ID : %s", evt.ReservationID)
	log.Printf("                  Start Time     : %s", evt.StartTime.Format("2006-01-02 15:04 MST"))
	log.Printf("                  End Time       : %s", evt.EndTime.Format("2006-01-02 15:04 MST"))
	log.Printf("                  Total Amount   : $%.2f", evt.TotalAmount)
}

func handlePaymentSuccess(body []byte, stats *Stats) {
	var evt PaymentStatusEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse payment.success event: %v | raw: %s", err, string(body))
		return
	}

	stats.PaymentSuccess.Add(1)

	// Mock email: Payment Receipt
	log.Printf("[EMAIL] 🧾  Payment Receipt Sent")
	log.Printf("         To:      User ID: %s", evt.UserID)
	log.Printf("         Subject: Payment Successful — Receipt for %s", evt.TransactionRef)
	log.Printf("         Body:    Your payment has been processed successfully.")
	log.Printf("                  Transaction Ref : %s", evt.TransactionRef)
	log.Printf("                  Amount          : %.2f %s", evt.Amount, evt.Currency)
	log.Printf("                  Reservation ID  : %s", evt.ReservationID)
	log.Printf("                  Status          : %s", evt.Status)
}

func handlePaymentFailed(body []byte, stats *Stats) {
	var evt PaymentStatusEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse payment.failed event: %v | raw: %s", err, string(body))
		return
	}

	stats.PaymentFailed.Add(1)

	// Mock email: Payment Failure Notice
	log.Printf("[EMAIL] ❌  Payment Failure Notice Sent")
	log.Printf("         To:      User ID: %s", evt.UserID)
	log.Printf("         Subject: Payment Failed — Action Required")
	log.Printf("         Body:    Unfortunately, your payment of %.2f %s could not be processed.",
		evt.Amount, evt.Currency)
	log.Printf("                  Transaction Ref : %s", evt.TransactionRef)
	log.Printf("                  Please retry with valid card details.")
}

func handlePaymentRefunded(body []byte, stats *Stats) {
	var evt PaymentStatusEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse payment.refunded event: %v | raw: %s", err, string(body))
		return
	}

	stats.PaymentRefunded.Add(1)

	// Mock email: Refund Confirmation
	log.Printf("[EMAIL] 💸  Refund Confirmation Sent")
	log.Printf("         To:      User ID: %s", evt.UserID)
	log.Printf("         Subject: Refund Processed — %s", evt.TransactionRef)
	log.Printf("         Body:    Your refund of %.2f %s has been processed.", evt.Amount, evt.Currency)
	log.Printf("                  Transaction Ref : %s", evt.TransactionRef)
	log.Printf("                  Please allow 3–5 business days for funds to appear.")
}

func handleReservationCancelled(body []byte, stats *Stats) {
	var evt ReservationCancelledEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse reservation.cancelled event: %v | raw: %s", err, string(body))
		return
	}

	stats.ReservationCancelled.Add(1)

	// Mock email: Cancellation Notification
	log.Printf("[EMAIL] 🚫  Cancellation Notification Sent")
	log.Printf("         To:      %s", evt.UserEmail)
	log.Printf("         Subject: Reservation Cancelled")
	log.Printf("         Body:    Your reservation [%s] has been cancelled.", evt.ReservationID)
	log.Printf("                  Reason: %s", evt.Reason)
}

func handleVehicleEntered(body []byte, stats *Stats) {
	var evt VehicleEntryEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse vehicle.entered event: %v | raw: %s", err, string(body))
		return
	}

	stats.VehicleEntered.Add(1)

	// Log entry
	log.Printf("[IOT]  🚗  Vehicle Entry Logged")
	log.Printf("            Vehicle ID      : %s", evt.VehicleID)
	log.Printf("            Parking Space   : %s", evt.ParkingSpaceID)
	log.Printf("            Entry Timestamp : %s", fmt.Sprint(evt.Timestamp))
}

func handleVehicleExited(body []byte, stats *Stats) {
	var evt VehicleExitEvent
	if err := json.Unmarshal(body, &evt); err != nil {
		log.Printf("[ERROR] Failed to parse vehicle.exited event: %v | raw: %s", err, string(body))
		return
	}

	stats.VehicleExited.Add(1)

	// Log exit + trigger billing note
	log.Printf("[IOT]  🏁  Vehicle Exit Logged")
	log.Printf("            Vehicle ID      : %s", evt.VehicleID)
	log.Printf("            Parking Space   : %s", evt.ParkingSpaceID)
	log.Printf("            Exit Timestamp  : %s", fmt.Sprint(evt.Timestamp))
	log.Printf("            Duration        : %d minutes", evt.DurationMins)
	log.Printf("            [BILLING]  Billing trigger queued for vehicle %s", evt.VehicleID)
}
