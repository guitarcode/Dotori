package season.blossom.dotori.roommate;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import season.blossom.dotori.error.errorcode.CommonErrorCode;
import season.blossom.dotori.error.exception.RestApiException;
import season.blossom.dotori.roommatecomment.*;
import season.blossom.dotori.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class RoommatePostService {
    private RoommatePostRepository roommatePostRepository;
    private RoommateCommentService roommateCommentService;
    private RoommateCommentSeqRepository roommateCommentSeqRepository;

    @Transactional
    public RoommatePost savePost(RoommatePostDto roommatePostDto) {

        RoommatePost savedPost = RoommatePost.builder()
                .writer(roommatePostDto.getWriter())
                .title(roommatePostDto.getTitle())
                .people(roommatePostDto.getPeople())
                .content(roommatePostDto.getContent())
                .roommateStatus(RoommateStatus.MATCHING)
                .build();

        RoommateCommentSeq roommateCommentSeq = RoommateCommentSeq.builder()
                .roommatePost(savedPost)
                .user(savedPost.getWriter())
                .writeSeq(savedPost.getNumberOfCommentWriter())
                .build();

        roommateCommentSeqRepository.save(roommateCommentSeq);

        return roommatePostRepository.save(savedPost);
    }


    @Transactional
    public List<RoommatePostReturnDto> getList(User user, int matchType) {
        List<RoommatePost> roommatePosts;

        if (matchType == 1) {
            roommatePosts = roommatePostRepository.findAllByWriter_UniversityAndRoommateStatusOrderByCreatedDateDesc(
                    user.getUniversity(), RoommateStatus.MATCHING);
        }
        else {
            roommatePosts = roommatePostRepository.findAllByWriter_UniversityOrderByCreatedDateDesc(
                    user.getUniversity());
        }
        return roommatePosts.stream().map(RoommatePostReturnDto::new).collect(Collectors.toList());
    }


    @Transactional
    public RoommatePostReturnDto getPost(Long userId, Long postId) {
        Optional<RoommatePost> roommatePostWrapper = roommatePostRepository.findById(postId);
        RoommatePost roommatePost = roommatePostWrapper.get();
        List<RoommateCommentReturnDto> comments = roommateCommentService.getComments(postId, userId);

        RoommatePostReturnDto roommatePostDto = RoommatePostReturnDto.builder()
                .id(roommatePost.getId())
                .title(roommatePost.getTitle())
                .people(roommatePost.getPeople())
                .content(roommatePost.getContent())
                .writer(roommatePost.getWriter().getEmail())
                .age(roommatePost.getWriter().getAge())
                .floor(roommatePost.getWriter().getFloor())
                .dorm(roommatePost.getWriter().getDorm())
                .gender(roommatePost.getWriter().getGender())
                .createdDate(roommatePost.getCreatedDate())
                .modifiedDate(roommatePost.getModifiedDate())
                .roommateStatus(roommatePost.getRoommateStatus())
                .comments(comments)
                .build();

        return roommatePostDto;
    }

    public RoommatePostReturnDto postMatchStatus(Long postId, Long userId){
        Optional<RoommatePost> byId = roommatePostRepository.findById(postId);
        RoommatePost roommatePost = byId.orElseThrow(() -> new RestApiException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if(roommatePost.getWriter().getUserId() == userId){
            roommatePost.setRoommateStatus(RoommateStatus.MATCHED);
        }

        RoommatePostReturnDto roommatePostDto = new RoommatePostReturnDto(roommatePost);

        return roommatePostDto;
    }

    @Transactional
    public RoommatePostReturnDto updatePost(Long postId, RoommatePostDto roommatePostDto, Long userId) {
        RoommatePost roommatePost = roommatePostRepository.findById(postId).orElseThrow(() -> new NullPointerException("해당 포스트가 존재하지 않습니다."));

        if (roommatePost.getWriter().getUserId().equals(userId)){
            roommatePost.setTitle(roommatePostDto.getTitle());
            roommatePost.setPeople(roommatePostDto.getPeople());
            roommatePost.setContent(roommatePostDto.getContent());

           return new RoommatePostReturnDto(roommatePost);
        }
        else {
            throw new RestApiException(CommonErrorCode.UNAUTHORIZED_USER);
        }
    }


    @Transactional
    public void deletePost(Long postId, Long userId) {
        Optional<RoommatePost> byId = roommatePostRepository.findById(postId);
        RoommatePost roommatePost = byId.orElseThrow(() -> new RestApiException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (roommatePost.getWriter().getUserId().equals(userId)){
            roommatePostRepository.deleteById(postId);
        }
        else {
            throw new RestApiException(CommonErrorCode.UNAUTHORIZED_USER);
        }
    }

    @Transactional
    public List<RoommatePostReturnDto> getMyList(User user) {
        List<RoommatePost> roommatePosts = roommatePostRepository.findAll();
        List<RoommatePostReturnDto> roommatePostList = new ArrayList<>();

        for (RoommatePost roommatePost : roommatePosts) {
            if (roommatePost.getWriter().getEmail().equals(user.getEmail())) {
                RoommatePostReturnDto roommatePostDto = RoommatePostReturnDto.builder()
                        .id(roommatePost.getId())
                        .title(roommatePost.getTitle())
                        .people(roommatePost.getPeople())
                        .content(roommatePost.getContent())
                        .writer(roommatePost.getWriter().getEmail())
                        .age(roommatePost.getWriter().getAge())
                        .floor(roommatePost.getWriter().getFloor())
                        .dorm(roommatePost.getWriter().getDorm())
                        .gender(roommatePost.getWriter().getGender())
                        .createdDate(roommatePost.getCreatedDate())
                        .modifiedDate(roommatePost.getModifiedDate())
                        .roommateStatus(roommatePost.getRoommateStatus())
                        .build();

                roommatePostList.add(roommatePostDto);
            } else continue;
        }
        return roommatePostList;
    }

    public List<RoommatePostReturnDto> getMyCommentList(User user) {
        return roommatePostRepository.findAllByCommentWriter(user)
                .stream().map(RoommatePostReturnDto::new)
                .collect(Collectors.toList());
    }

    public RoommatePostReturnDto getPostForUpdate(Long postId, User user) {
        Optional<RoommatePost> byId = roommatePostRepository.findById(postId);
        RoommatePost roommatePost = byId.orElseThrow(() -> new RestApiException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if(roommatePost.getWriter().getUserId() != user.getUserId()){
            throw new RestApiException(CommonErrorCode.UNAUTHORIZED_USER);
        }

        return new RoommatePostReturnDto(roommatePost);
    }

}