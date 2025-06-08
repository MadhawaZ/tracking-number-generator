package org.example.service;

/**
 * Service interface for generating unique tracking numbers
 */
public interface TrackingNumberService {

    /**
     * Generate a unique tracking number based on shipment parameters
     * @param originCountryId Origin country code in ISO 3166-1 alpha-2 format
     * @param destinationCountryId Destination country code in ISO 3166-1 alpha-2 format
     * @param weight Package weight in kilograms
     * @param customerId Customer UUID
     * @return Unique tracking number matching pattern ^[A-Z0-9]{1,16}$
     */
    String generateTrackingNumber(String originCountryId, String destinationCountryId,
                                 double weight, String customerId);
}