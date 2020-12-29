package com.chan.ws.mobileappws.service;

import com.chan.ws.mobileappws.shared.dto.AddressDTO;

import java.util.List;

public interface AddressService {
    List<AddressDTO> getAddresses(String userId);
    AddressDTO getAddress(String addressId);
}
