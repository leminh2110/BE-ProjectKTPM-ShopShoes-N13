package com.microservice.invoiceservice.controller;

import com.microservice.invoiceservice.dto.ApiResponse;
import com.microservice.invoiceservice.dto.InvoiceRequest;
import com.microservice.invoiceservice.dto.InvoiceResponse;
import com.microservice.invoiceservice.entity.Invoice;
import com.microservice.invoiceservice.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);
    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<ApiResponse<Invoice>> createInvoice(@RequestBody InvoiceRequest request) {
        try {
            log.info("Creating invoice with request: {}", request);
            Invoice savedInvoice = invoiceService.createInvoice(request);
            log.info("Created invoice with ID: {}, shipAddress: '{}'", savedInvoice.getId(), savedInvoice.getShipAddress());
            
            ApiResponse<Invoice> response = new ApiResponse<>(
                "Invoice created successfully!",
                "SUCCESS",
                savedInvoice
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(e.getMessage(), "ERROR", null));
        } catch (Exception e) {
            log.error("Failed to create invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to create invoice: " + e.getMessage(), "ERROR", null));
        }
    }

    @PostMapping("/create-from-cart")
    public ResponseEntity<ApiResponse<Invoice>> createInvoiceFromCart(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received request to create invoice from cart: {}", request);
            
            // Validate required fields
            if (!request.containsKey("userId") || !request.containsKey("shipAddress")) {
                throw new IllegalArgumentException("userId and shipAddress are required fields");
            }
            
            // Create InvoiceRequest from Map
            InvoiceRequest invoiceRequest = new InvoiceRequest();
            invoiceRequest.setUserId(Long.valueOf(request.get("userId").toString()));
            invoiceRequest.setShipAddress(request.get("shipAddress").toString());
            invoiceRequest.setStatus("PENDING");
            
            // Set total amount if provided
            if (request.containsKey("totalAmount")) {
                invoiceRequest.setTotalAmount(new BigDecimal(request.get("totalAmount").toString()));
            }
            
            // Set items
            if (!request.containsKey("items")) {
                throw new IllegalArgumentException("Cart items are required");
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Cart items cannot be empty");
            }
            invoiceRequest.setItems(items);
            
            // Create invoice
            Invoice savedInvoice = invoiceService.createInvoiceFromCart(invoiceRequest);
            log.info("Created invoice with ID: {}, shipAddress: '{}'", savedInvoice.getId(), savedInvoice.getShipAddress());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>("Invoice created successfully!", "SUCCESS", savedInvoice));
                    
        } catch (IllegalArgumentException e) {
            log.error("Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(e.getMessage(), "ERROR", null));
        } catch (Exception e) {
            log.error("Failed to create invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to create invoice: " + e.getMessage(), "ERROR", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id);
            InvoiceResponse response = InvoiceResponse.fromEntity(invoice);
            return ResponseEntity.ok(new ApiResponse<>("Invoice retrieved successfully!", "SUCCESS", response));
        } catch (Exception e) {
            log.error("Failed to get invoice: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to get invoice: " + e.getMessage(), "ERROR", null));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByUserId(@PathVariable Long userId) {
        try {
            List<Invoice> invoices = invoiceService.getInvoicesByUserId(userId);
            List<InvoiceResponse> responses = invoices.stream()
                    .map(InvoiceResponse::fromEntity)
                    .toList();
            return ResponseEntity.ok(new ApiResponse<>("Invoices retrieved successfully!", "SUCCESS", responses));
        } catch (Exception e) {
            log.error("Failed to get invoices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to get invoices: " + e.getMessage(), "ERROR", null));
        }
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByStatus(@PathVariable String status) {
        try {
            List<Invoice> invoices = invoiceService.getInvoicesByStatus(status);
            List<InvoiceResponse> responses = invoices.stream()
                    .map(InvoiceResponse::fromEntity)
                    .toList();
            return ResponseEntity.ok(new ApiResponse<>("Invoices retrieved successfully!", "SUCCESS", responses));
        } catch (Exception e) {
            log.error("Failed to get invoices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to get invoices: " + e.getMessage(), "ERROR", null));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoiceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            if (!request.containsKey("status")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("Status is required", "ERROR", null));
            }

            Invoice updatedInvoice = invoiceService.updateInvoiceStatus(id, request.get("status"), request.get("transactionId"));
            InvoiceResponse response = InvoiceResponse.fromEntity(updatedInvoice);
            return ResponseEntity.ok(new ApiResponse<>("Invoice status updated successfully!", "SUCCESS", response));
        } catch (Exception e) {
            log.error("Failed to update invoice status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("Failed to update invoice status: " + e.getMessage(), "ERROR", null));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        ApiResponse<Void> response = new ApiResponse<>(
            "Invoice deleted successfully!",
            "SUCCESS",
            null
        );
        return ResponseEntity.ok(response);
    }
} 