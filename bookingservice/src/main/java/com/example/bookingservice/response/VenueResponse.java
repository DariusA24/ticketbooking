package com.example.bookingservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueResponse {
	private Long venueId;
	private String venueName;
	private Long totalCapacity;
}
