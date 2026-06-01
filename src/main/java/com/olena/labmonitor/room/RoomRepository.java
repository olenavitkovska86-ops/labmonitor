package com.olena.labmonitor.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
            select room
            from Room room
            where room.lab.id = :labId
            order by room.id asc
            """)
    List<Room> findByLabId(@Param("labId") Long labId);

    @Query("""
            select room
            from Room room
            where lower(room.name) like lower(concat('%', :name, '%'))
            order by room.id asc
            """)
    List<Room> searchByName(@Param("name") String name);

    @Query("""
            select room
            from Room room
            where room.lab.id = :labId
              and lower(room.name) like lower(concat('%', :name, '%'))
            order by room.id asc
            """)
    List<Room> searchByLabIdAndName(
            @Param("labId") Long labId,
            @Param("name") String name
    );
}
