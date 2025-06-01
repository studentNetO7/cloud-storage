package com.semenova.cloudstorage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "files")
public class File {

    @Setter
    @Getter
    @Id
    private UUID id;

    @Setter
    @Getter
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Getter
    @Setter
    @Column(nullable = false)
    private String filename;

    @Setter
    @Getter
    @Column(nullable = false)
    private long size;

    @Getter
    @Setter
    @Column(name = "upload_date")
    private LocalDateTime uploadDate;

    @Getter
    @Setter
    @Column(nullable = false)
    private String path;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (uploadDate == null) uploadDate = LocalDateTime.now();
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}

