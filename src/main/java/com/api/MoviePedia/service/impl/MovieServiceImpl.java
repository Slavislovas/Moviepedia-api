package com.api.MoviePedia.service.impl;

import com.api.MoviePedia.builder.MovieSpecificationBuilder;
import com.api.MoviePedia.enumeration.Role;
import com.api.MoviePedia.exception.DuplicateDatabaseEntryException;
import com.api.MoviePedia.model.movie.MovieCreationDto;
import com.api.MoviePedia.model.movie.MovieRetrievalDto;
import com.api.MoviePedia.model.movie.SearchCriteriaDto;
import com.api.MoviePedia.model.movie.SearchDto;
import com.api.MoviePedia.repository.MovieRepository;
import com.api.MoviePedia.repository.RatingRepository;
import com.api.MoviePedia.repository.model.ActorEntity;
import com.api.MoviePedia.repository.model.DirectorEntity;
import com.api.MoviePedia.repository.model.ImgurImageEntity;
import com.api.MoviePedia.repository.model.MovieEntity;
import com.api.MoviePedia.repository.model.RatingEntity;
import com.api.MoviePedia.repository.model.UserEntity;
import com.api.MoviePedia.service.ActorService;
import com.api.MoviePedia.service.DirectorService;
import com.api.MoviePedia.service.FileStorageService;
import com.api.MoviePedia.service.MovieService;
import com.api.MoviePedia.service.UserService;
import com.api.MoviePedia.util.mapper.MovieMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MovieServiceImpl implements MovieService {
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final MovieMapper movieMapper;
    private final FileStorageService fileStorageService;
    private final DirectorService directorService;
    private final ActorService actorService;
    private final UserService userService;

    @Override
    public List<MovieRetrievalDto> getAllMovies() {
        return movieRepository
                .findAll()
                .stream()
                .map(movieEntity -> movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink()))
                .collect(Collectors.toList());
    }

    @Override
    public MovieRetrievalDto getMovieById(Long directorId, Long movieId) {
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Movie with id: " + movieId  + " does not exist"));

        MovieRetrievalDto movieRetrievalDto = movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Role role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(x -> Role.valueOf(x.getAuthority())).toList().get(0);
        if (!role.name().equals("ROLE_ANONYMOUS")){
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            UserEntity user = userService.getUserEntityById(userId);
            RatingEntity userRatingForMovie = getUserRatingForMovie(userId, movieEntity);
            if (userRatingForMovie != null){
                movieRetrievalDto.setUserRatingForMovie(userRatingForMovie.getRating());
            } else{
                movieRetrievalDto.setUserRatingForMovie(0);
            }
            movieRetrievalDto.setMovieInUserWatchlist(user.getWatchlist().stream().anyMatch(watchlistMovie -> watchlistMovie.getId().equals(movieRetrievalDto.getId())));
            movieRetrievalDto.setMovieInUserWatchedMovies(user.getWatchedMovies().stream().anyMatch(watchedMovie -> watchedMovie.getId().equals(movieRetrievalDto.getId())));
        } else{
            movieRetrievalDto.setUserRatingForMovie(0);
        }
        return movieRetrievalDto;
    }

    @Override
    public MovieEntity getMovieEntityById(Long movieId) {
        Optional<MovieEntity> optionalMovieEntity = movieRepository.findById(movieId);
        if (optionalMovieEntity.isEmpty()){
            throw new NoSuchElementException("Movie with id: " + movieId  + " does not exist");
        }
        return optionalMovieEntity.get();
    }

    @Override
    public Set<MovieRetrievalDto> getAllMoviesByDirectorId(Long directorId) {
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        return directorEntity
                .getMovies()
                .stream()
                .map(movieEntity -> movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink()))
                .collect(Collectors.toSet());
    }

    @Override
    public MovieRetrievalDto createMovie(Long directorId, MovieCreationDto movieCreationDto) throws IOException {
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        ImgurImageEntity imgurImageEntity = fileStorageService.saveFile(movieCreationDto.getPicture());
        MovieEntity movieEntity = movieMapper.creationDtoToEntity(movieCreationDto, null, imgurImageEntity, 0, 0, 0.0, directorEntity, new HashSet<>(), new HashSet<>());
        movieEntity = movieRepository.save(movieEntity);
        return movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink());
    }

    @Override
    public MovieRetrievalDto editMovieById(Long movieId, Long directorId, MovieCreationDto movieCreationDto) throws IOException {
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        ImgurImageEntity imgurImageEntity = editMoviePicture(movieCreationDto, movieEntity);
        movieEntity = movieMapper.creationDtoToEntity(movieCreationDto, movieEntity.getId(), imgurImageEntity, movieEntity.getTotalRating(),
                movieEntity.getTotalVotes(),movieEntity.getRating(), directorEntity, movieEntity.getActors(), movieEntity.getReviews());
        movieEntity = movieRepository.save(movieEntity);
        return movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink());
    }

    private ImgurImageEntity editMoviePicture(MovieCreationDto newMovieData, MovieEntity oldMovieData) throws IOException {
        fileStorageService.deleteFileByHash(oldMovieData.getImgurImageEntity().getId());
        return fileStorageService.saveFile(newMovieData.getPicture());
    }

    @Override
    public MovieRetrievalDto setMovieActors(Long directorId, Long movieId, Set<Long> actorIds) {
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        if (actorIds != null){
            Set<ActorEntity> actorEntities = new HashSet<>();
            for (Long actorId : actorIds) {
                actorEntities.add(actorService.getActorEntityById(actorId));
            }
            movieEntity.setActors(actorEntities);
        } else{
            movieEntity.setActors(new HashSet<>());
        }
        movieEntity = movieRepository.save(movieEntity);
        return movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink());
    }

    @Override
    public void deleteMovieById(Long directorId, Long movieId) {
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        removeActorsFromMovie(movieEntity);
        removeMovieFromWatchlists(movieEntity);
        removeMovieFromWatchedMovies(movieEntity);
        fileStorageService.deleteFileByHash(movieEntity.getImgurImageEntity().getId());
        movieRepository.deleteById(movieId);
    }

    private void removeActorsFromMovie(MovieEntity movieEntity) {
        for (Iterator<ActorEntity> iterator = movieEntity.getActors().iterator(); iterator.hasNext();){
            ActorEntity actorEntity = iterator.next();
            iterator.remove();
            actorEntity.getMovies().remove(movieEntity);
        }
    }

    private void removeMovieFromWatchlists(MovieEntity movieEntity) {
        for (Iterator<UserEntity> iterator = movieEntity.getUsersWithMovieInWatchlist().iterator(); iterator.hasNext();){
            UserEntity userEntity = iterator.next();
            iterator.remove();
            userEntity.getWatchlist().remove(movieEntity);
        }
    }

    private void removeMovieFromWatchedMovies(MovieEntity movieEntity){
        for (Iterator<UserEntity> iterator = movieEntity.getUsersWhoHaveWatchedMovie().iterator(); iterator.hasNext();){
            UserEntity userEntity = iterator.next();
            iterator.remove();
            userEntity.getWatchedMovies().remove(movieEntity);
        }
    }

    @Override
    public List<MovieRetrievalDto> getMoviesBySearchCriteria(SearchDto searchDto, Integer pageNumber, Integer pageSize) {
        MovieSpecificationBuilder movieSpecificationBuilder = new MovieSpecificationBuilder();
        List<SearchCriteriaDto> criteriaList = searchDto.getSearchCriteriaList();
        if (criteriaList != null){
            criteriaList.forEach(criteria -> {
                criteria.setDataOption(searchDto.getDataOption());
                movieSpecificationBuilder.with(criteria);
            });
        }
        Specification<MovieEntity> movieSpecification = movieSpecificationBuilder.build();
        Pageable page = PageRequest.of(pageNumber, pageSize, Sort.by("year").descending());
        Page<MovieEntity> moviePage = movieRepository.findAll(movieSpecification, page);
        return moviePage.toList().stream().map(movieEntity -> movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink())).collect(Collectors.toList());
    }

    @Override
    public void addMovieToWatchedMovies(Long directorId, Long movieId) {
       Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
       DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
       MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
       UserEntity userEntity = userService.getUserEntityById(userId);
       userEntity.getWatchedMovies().add(movieEntity);
       movieRepository.save(movieEntity);
    }

    @Override
    public void addMovieToWatchlist(Long directorId, Long movieId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        UserEntity userEntity = userService.getUserEntityById(userId);
        userEntity.getWatchlist().add(movieEntity);
        movieRepository.save(movieEntity);
    }

    @Override
    public void deleteMovieFromWatchedMovies(Long directorId, Long movieId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        UserEntity userEntity = userService.getUserEntityById(userId);
        userEntity.getWatchedMovies().remove(movieEntity);
        movieRepository.save(movieEntity);
    }

    @Override
    public void deleteMovieFromWatchlist(Long directorId, Long movieId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        UserEntity userEntity = userService.getUserEntityById(userId);
        userEntity.getWatchlist().remove(movieEntity);
        movieRepository.save(movieEntity);
    }

    @Override
    public Set<MovieRetrievalDto> getWatchedMoviesByUserId() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity userEntity = userService.getUserEntityById(userId);
        return userEntity.getWatchedMovies().stream().map(movieEntity -> movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink())).collect(Collectors.toSet());
    }

    @Override
    public Set<MovieRetrievalDto> getWatchlistByUserId() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity userEntity = userService.getUserEntityById(userId);
        return userEntity.getWatchlist().stream().map(movieEntity -> movieMapper.entityToRetrievalDto(movieEntity, movieEntity.getImgurImageEntity().getLink())).collect(Collectors.toSet());
    }

    @Override
    public void rateMovieById(Long directorId, Long movieId, Integer rating) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        DirectorEntity directorEntity = directorService.getDirectorEntityById(directorId);
        MovieEntity movieEntity = directorEntity
                .getMovies()
                .stream()
                .filter(movie -> movie.getId().equals(movieId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Director has not made a movie with id: " + movieId));
        if(checkIfUserAlreadyRatedMovie(userId, movieEntity)){
            RatingEntity userRating = getUserRatingForMovie(userId, movieEntity);
            if (userRating != null){
                movieEntity.setTotalRating(movieEntity.getTotalRating() - userRating.getRating());
                movieEntity.setTotalVotes(movieEntity.getTotalVotes() - 1);
                movieEntity.getRatings().remove(userRating);
                ratingRepository.deleteById(userRating.getId());
            }
        }
        movieEntity.rateMovie(rating);
        UserEntity userEntity = userService.getUserEntityById(userId);
        RatingEntity ratingEntity = new RatingEntity(null, rating, userEntity, movieEntity);
        movieRepository.save(movieEntity);
        ratingRepository.save(ratingEntity);
    }

    @Override
    public Integer getRatingByUserIdAndMovieId(Long userId, Long movieId) {
        //validateUserPermissions(userId, "Users can only retrieve their own rating for the movie");
        Optional<MovieEntity> optionalMovieEntity = movieRepository.findById(movieId);
        if (optionalMovieEntity.isEmpty()){
            throw new NoSuchElementException("Movie with id: " + movieId  + " does not exist");
        }
        MovieEntity movieEntity = optionalMovieEntity.get();
        for (RatingEntity rating : movieEntity.getRatings()) {
            if (rating.getUser().getId().equals(userId)){
                return rating.getRating();
            }
        }
        throw new NoSuchElementException("User with id: " + userId + " has not rated this movie");
    }

    private Boolean checkIfUserAlreadyRatedMovie(Long userId, MovieEntity movieEntity) {
        for (RatingEntity ratingEntity : movieEntity.getRatings()) {
            if (ratingEntity.getUser().getId().equals(userId)){
                return true;
            }
        }
        return false;
    }

    private RatingEntity getUserRatingForMovie(Long userId, MovieEntity movieEntity){
        for (RatingEntity ratingEntity : movieEntity.getRatings()) {
            if (ratingEntity.getUser().getId().equals(userId)){
                return ratingEntity;
            }
        }
        return null;
    }

    private void validateUserPermissions(Long userId, String errorMessage) {
        Role role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().map(x -> Role.valueOf(x.getAuthority())).toList().get(0);
        if (role == Role.ROLE_USER){
            Long authenticatedUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!Objects.equals(authenticatedUserId, userId)){
                throw new SecurityException(errorMessage);
            }
        }
    }
}
