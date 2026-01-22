package com.example.bookingservice.service;

import com.example.bookingservice.client.InventoryServiceClient;
import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import com.example.bookingservice.response.InventoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class BookingService {

	private final CustomerRepository customerRepository;
	private final InventoryServiceClient inventoryServiceClient;
	private final KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate;

	@Autowired
	public BookingService(final CustomerRepository customerRepository,
												final InventoryServiceClient inventoryServiceClient,
												final KafkaTemplate<String, BookingEvent> bookingEventKafkaTemplate) {
		this.customerRepository = customerRepository;
		this.inventoryServiceClient = inventoryServiceClient;
		this.bookingEventKafkaTemplate = bookingEventKafkaTemplate;
	}

	public BookingResponse createBooking(final BookingRequest request) {
		//check if user exists
		final Customer customer = customerRepository.findById(request.getUserId()).orElse(null);
		if (customer == null) {
			throw new RuntimeException("Customer not found");
		}
		//check if there is enough inventory
		final InventoryResponse inventoryResponse = inventoryServiceClient.getInventory(request.getUserId());
		log.info("Inventory Service Response" + inventoryResponse);
		if (inventoryResponse.getCapacity() < request.getTicketCount()) {
			throw new RuntimeException(("Not enough inventory"));
		}
		// create booking
		final BookingEvent bookingEvent = createBookingEvent(request,customer, inventoryResponse);
		// send booking to Order service on a kafka topic
		bookingEventKafkaTemplate.send("booking", bookingEvent);
		log.info("Booking Event sent to Kafka");

		return BookingResponse.builder()
				.userId(bookingEvent.getUserId())
				.eventId(bookingEvent.getEventId())
				.ticketCount(bookingEvent.getTicketCount())
				.totalPrice(bookingEvent.getTotalPrice())
				.build();
	}

	private BookingEvent createBookingEvent(final BookingRequest request, final Customer customer, final InventoryResponse inventoryResponse) {
		return BookingEvent.builder()
				.userId(customer.getId())
				.eventId(request.getEventId())
				.ticketCount(request.getTicketCount())
				.totalPrice(inventoryResponse.getTicketPrice().multiply(BigDecimal.valueOf(request.getTicketCount())))
				.build();
	}
}
