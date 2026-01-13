package com.inghubs.digitalwallet.utilities.mappers;

import org.mapstruct.Mapper;

import com.inghubs.digitalwallet.dtos.requests.CreateWalletRequest;
import com.inghubs.digitalwallet.entities.Wallet;

@Mapper(componentModel = "spring")
public interface CreateWalletMapper extends BaseMapper<CreateWalletRequest, Wallet> {

}
