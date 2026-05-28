package com.olena.labmonitor.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByLabIdOrderByIdAsc(Long labId);

    List<Room> findByNameContainingIgnoreCaseOrderByIdAsc(String name);

    List<Room> findByLabIdAndNameContainingIgnoreCaseOrderByIdAsc(Long labId, String name);
}
