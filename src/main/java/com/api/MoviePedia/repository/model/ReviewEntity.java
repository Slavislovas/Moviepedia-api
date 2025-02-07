package com.api.MoviePedia.repository.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "reviews")
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "text")
    private String text;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity reviewer;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private MovieEntity movie;

    @OneToMany(mappedBy = "review", cascade = CascadeType.REMOVE)
    private Set<LikeEntity> likes;

    @OneToMany(mappedBy = "review", cascade = CascadeType.REMOVE)
    private Set<DislikeEntity> dislikes;
}
