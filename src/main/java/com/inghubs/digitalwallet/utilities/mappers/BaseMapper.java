package com.inghubs.digitalwallet.utilities.mappers;

public interface BaseMapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);
}
