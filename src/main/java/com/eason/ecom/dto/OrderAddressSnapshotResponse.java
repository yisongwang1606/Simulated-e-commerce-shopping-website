package com.eason.ecom.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderAddressSnapshotResponse", description = "Shipping address snapshot stored on the order at the time it was created")
public record OrderAddressSnapshotResponse(
        String receiverName,
        String phone,
        String line1,
        String line2,
        String city,
        String province,
        String postalCode) {
}
