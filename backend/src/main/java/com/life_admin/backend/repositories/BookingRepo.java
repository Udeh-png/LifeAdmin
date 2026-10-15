package com.life_admin.backend.repositories;

import com.life_admin.backend.models.Booking;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.mongodb.repository.MongoRepository;

@NullMarked
public interface BookingRepo extends MongoRepository<Booking, String> {
}
