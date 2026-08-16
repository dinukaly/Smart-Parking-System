package consumer

import (
	"fmt"
	"log"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
	"notification-service/internal/handler"
)



// Binding definitions

// routingKeyBindings lists all topic routing key patterns to bind to notification.queue.
// Topic wildcards:  *  matches exactly one word,  #  matches zero or more words.
var routingKeyBindings = []string{
	"parking.*",      // parking.reserved, parking.updated, etc.
	"payment.*",      // payment.success, payment.failed, payment.refunded
	"reservation.*",  // reservation.cancelled, reservation.created, etc.
	"vehicle.*",      // vehicle.entered, vehicle.exited
}



// Consumer — connection, retry, and consume loop

// Consumer manages the RabbitMQ connection lifecycle.
type Consumer struct {
	amqpURL    string
	exchange   string
	queue      string
	stats      *handler.Stats
	conn       *amqp.Connection
	channel    *amqp.Channel
}

// New creates a new Consumer instance.
func New(amqpURL, exchange, queue string, stats *handler.Stats) *Consumer {
	return &Consumer{
		amqpURL:  amqpURL,
		exchange: exchange,
		queue:    queue,
		stats:    stats,
	}
}

// Start connects to RabbitMQ with retry and begins consuming messages.
// It blocks the calling goroutine and automatically reconnects on failure.
func (c *Consumer) Start() {
	for {
		if err := c.connect(); err != nil {
			log.Printf("[AMQP]  Connection failed: %v — retrying in 5s...", err)
			time.Sleep(5 * time.Second)
			continue
		}

		log.Println("[AMQP]  Connected to RabbitMQ. Starting consumer...")

		// Block until the channel or connection is closed
		if err := c.consume(); err != nil {
			log.Printf("[AMQP]  Consumer stopped: %v — reconnecting in 5s...", err)
		}

		c.cleanup()
		time.Sleep(5 * time.Second)
	}
}



// Internal: connect and setup

// connect establishes the AMQP connection and channel, then declares the topology.
func (c *Consumer) connect() error {
	var (
		conn *amqp.Connection
		ch   *amqp.Channel
		err  error
	)

	// Dial with retries
	maxRetries := 5
	for i := 1; i <= maxRetries; i++ {
		conn, err = amqp.Dial(c.amqpURL)
		if err == nil {
			break
		}
		waitSec := time.Duration(i*3) * time.Second
		log.Printf("[AMQP]  Dial attempt %d/%d failed: %v — waiting %s", i, maxRetries, err, waitSec)
		time.Sleep(waitSec)
	}
	if err != nil {
		return err
	}

	ch, err = conn.Channel()
	if err != nil {
		conn.Close()
		return err
	}

	// Declare topology

	// 1. Declare the shared topic exchange (idempotent — already exists on the broker)
	if err = ch.ExchangeDeclare(
		c.exchange, // name
		"topic",    // kind
		true,       // durable
		false,      // auto-delete
		false,      // internal
		false,      // no-wait
		nil,        // args
	); err != nil {
		ch.Close()
		conn.Close()
		return err
	}

	// 2. Declare a durable queue for this service
	if _, err = ch.QueueDeclare(
		c.queue, // name
		true,    // durable
		false,   // delete when unused
		false,   // exclusive
		false,   // no-wait
		nil,     // args
	); err != nil {
		ch.Close()
		conn.Close()
		return err
	}

	// 3. Bind queue to exchange for each routing key pattern
	for _, key := range routingKeyBindings {
		if err = ch.QueueBind(
			c.queue,    // queue name
			key,        // routing key
			c.exchange, // exchange
			false,
			nil,
		); err != nil {
			ch.Close()
			conn.Close()
			return err
		}
		log.Printf("[AMQP]  Bound queue '%s' with key '%s' on exchange '%s'", c.queue, key, c.exchange)
	}

	// 4. Set QoS: process one message at a time (prevents message flooding)
	if err = ch.Qos(1, 0, false); err != nil {
		ch.Close()
		conn.Close()
		return err
	}

	c.conn = conn
	c.channel = ch
	return nil
}

// consume registers a consumer and blocks until the channel is closed.
func (c *Consumer) consume() error {
	deliveries, err := c.channel.Consume(
		c.queue,           // queue
		"notification-svc", // consumer tag
		false,             // auto-ack (we manually ack/nack)
		false,             // exclusive
		false,             // no-local
		false,             // no-wait
		nil,               // args
	)
	if err != nil {
		return err
	}

	// Watch for connection-level close signals
	connClose := c.conn.NotifyClose(make(chan *amqp.Error, 1))

	log.Printf("[AMQP]  Consuming from queue '%s' — waiting for messages...", c.queue)

	for {
		select {
		case d, ok := <-deliveries:
			if !ok {
				return fmt.Errorf("delivery channel closed")
			}
			c.processDelivery(d)

		case err := <-connClose:
			if err != nil {
				return fmt.Errorf("connection closed: %v", err)
			}
			return fmt.Errorf("connection closed gracefully")
		}
	}
}

// processDelivery dispatches the message and acks/nacks appropriately.
func (c *Consumer) processDelivery(d amqp.Delivery) {
	routingKey := d.RoutingKey
	log.Printf("[AMQP]  Received message: routingKey=%s bodyLen=%d", routingKey, len(d.Body))

	handler.Dispatch(routingKey, d.Body, c.stats)

	// Acknowledge the message — removes it from the queue
	if err := d.Ack(false); err != nil {
		log.Printf("[AMQP]  Failed to ack message (routingKey=%s): %v", routingKey, err)
	}
}

// cleanup closes channel and connection gracefully.
func (c *Consumer) cleanup() {
	if c.channel != nil {
		_ = c.channel.Close()
	}
	if c.conn != nil && !c.conn.IsClosed() {
		_ = c.conn.Close()
	}
	c.channel = nil
	c.conn = nil
}
