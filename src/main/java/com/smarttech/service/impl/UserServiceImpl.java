package com.smarttech.service.impl;

import com.smarttech.dto.request.UserRegistrationRequest;
import com.smarttech.dto.response.UserResponse;
import com.smarttech.entity.User;
import com.smarttech.enums.CustomerTier;
import com.smarttech.enums.UserRole;
import com.smarttech.exception.ResourceNotFoundException;
import com.smarttech.exception.UserAlreadyExistsException;
import com.smarttech.mapper.UserMapper;
import com.smarttech.repository.UserRepository;
import com.smarttech.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("User with phone number " + request.getPhoneNumber() + " already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(UserRole.CUSTOMER)
                .customerTier(CustomerTier.BRONZE)
                .totalPurchases(BigDecimal.ZERO)
                .isActive(true)
                .emailVerified(false)
                .phoneVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findEntityById(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = findEntityByEmail(email);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllCustomers(Pageable pageable) {
        Page<User> users = userRepository.findActiveUsersByRole(UserRole.CUSTOMER, pageable);
        return users.map(userMapper::toResponse);
    }

    @Override
    public UserResponse updateCustomerTier(Long userId, BigDecimal totalPurchases) {
        log.info("Updating customer tier for user ID: {} with total purchases: {}", userId, totalPurchases);

        User user = findEntityById(userId);
        user.setTotalPurchases(totalPurchases);
        
        CustomerTier newTier = CustomerTier.calculateTier(totalPurchases);
        if (newTier != user.getCustomerTier()) {
            log.info("Customer tier updated from {} to {} for user ID: {}", 
                    user.getCustomerTier(), newTier, userId);
            user.setCustomerTier(newTier);
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void verifyEmail(String email) {
        User user = findEntityByEmail(email);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for user: {}", email);
    }

    @Override
    public void verifyPhone(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone number: " + phoneNumber));
        user.setPhoneVerified(true);
        userRepository.save(user);
        log.info("Phone verified for user: {}", phoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public User findEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
