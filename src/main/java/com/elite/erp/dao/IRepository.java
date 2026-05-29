package com.elite.erp.dao;

import com.elite.erp.util.Response;
import java.util.List;

/**
 * Generic repository interface — demonstrates Java Generics + Polymorphism.
 * Every concrete DAO implements this interface, enabling polymorphic usage.
 *
 * @param <T> Domain entity type
 */
public interface IRepository<T> {
    Response<T>         save(T entity);
    Response<T>         findById(int id);
    Response<List<T>>   findAll();
    Response<Boolean>   update(T entity);
    Response<Boolean>   delete(int id);
}
