package com.sns.pet.controller;

import com.sns.pet.dto.JoinDto;
import com.sns.pet.dto.UserDto;
import com.sns.pet.dto.UserPetDto;
import com.sns.pet.service.EmailService;
import com.sns.pet.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Api("User 컨트롤러")
@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"*"}, maxAge = 6000)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";

    private final UserService userService;
    private final EmailService emailService;

    @ApiOperation(value = "회원가입", notes = "회원가입을 한다. DB 성공 여부에 따라 SUCCESS, FAIL 반환", response = String.class)
    @PostMapping
    public ResponseEntity<String> join(@RequestBody @ApiParam(value = "회원정보", required = true) JoinDto joinDto) throws Exception {
        logger.info("join - 호출");
        System.out.println(joinDto.toString());
        if (userService.addUser(joinDto)) {
            return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
        }
        return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "회원정보 조회", notes = "회원정보를 조회한다.", response = UserDto.class)
    @GetMapping("/{userNumber}")
    public ResponseEntity<UserDto> userDetails(@PathVariable("userNumber") @ApiParam(value = "조회할 회원번호") Long userNumber) throws Exception {
        logger.info("userDetails 호출");
        UserDto userDto = userService.findUser(userNumber);

        // 이미지 반환
        System.out.println(userDto.getSaveFolder() + userDto.getUserPhotoName());
        InputStream imageStream = new FileInputStream(userDto.getSaveFolder() + userDto.getUserPhotoName());
        userDto.setUserProfilePhoto(IOUtils.toByteArray(imageStream));

        return new ResponseEntity<UserDto>(userDto, HttpStatus.OK);
    }

//    @ApiOperation(value = "회원정보 수정", notes = "회원정보를 수정 한다. DB 성공 여부에 따라 SUCCESS, FAIL 반환", response = String.class)
//    @PutMapping
//    public ResponseEntity<String> userModify(@RequestBody @ApiParam(value = "수정할 회원정보") UserDto userDto) throws Exception {
//        logger.info("userModify 호출");
//        if (userService.modifyUser(userDto)) {
//            return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
//        }
//        return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
//    }

    @ApiOperation(value = "회원정보 수정", notes = "회원정보를 수정 한다. DB 성공 여부에 따라 SUCCESS, FAIL 반환", response = String.class)
    @PutMapping
    public ResponseEntity<String> userModify(
            @ApiParam(value = "회원 번호") @RequestPart(value = "userNumber") Long userNumber,
            @ApiParam(value = "회원 비밀번호 ") @RequestPart(value = "userPW", required = false) String userPW,
            @ApiParam(value = "회원 닉네임 ") @RequestPart(value = "userNickName", required = false) String userNickName,
            @ApiParam(value = "반려동물 유/무 ") @RequestPart(value = "petCheck", required = false) boolean petCheck,
            @ApiParam(value = "회원 프로필 소개글") @RequestPart(value = "userProfileContent", required = false) String userProfileContent,
            @ApiParam(value = "프로필 사진") @RequestPart(value = "userPhoto", required = false) MultipartFile image) throws Exception {
        logger.info("userModify 호출");
        UserDto userDto = new UserDto();
        userDto.setUserNumber(userNumber);
        userDto.setUserPW(userPW);
        userDto.setUserNickName(userNickName);
        userDto.setPetCheck(petCheck);
        userDto.setUserProfileContent(userProfileContent);

        if (image != null) {
            logger.info("file 확인");
            String originName, fileExtension, saveFileName, saveFolder;

//            saveFolder = File.separator + "Users" + File.separator + "leejuhyeong" + File.separator + "test" + File.separator + "profile" + File.separator; // 맥용
            saveFolder = "C:" + File.separator + "PJT" + File.separator + "test" + File.separator;                              // 윈도우용
            //saveFolder = File.separator + "home" + File.separator + "test" + File.separator + "profile" + File.separator;       // ec2 서버용

            logger.info("저장경로 확인 : {}", saveFolder);
            userDto.setSaveFolder(saveFolder);
            File folder = new File(saveFolder);
            if (!folder.exists()) {
                logger.info("폴더 생성 : {}", saveFolder);
                folder.mkdirs();
            }

            // 파일
            originName = image.getOriginalFilename();
            if (originName != null) {
                fileExtension = originName.substring(originName.lastIndexOf(".") + 1);  // 확장자
                originName = originName.substring(0, originName.lastIndexOf("."));      // 파일이름
                saveFileName = UUID.randomUUID() + originName + "." + fileExtension;
                userDto.setUserPhotoName(saveFileName);
                logger.info("파일 이름 생성 : {}", saveFileName);

                logger.info("file 저장");
                image.transferTo(new File(saveFolder, saveFileName));
            }

        }
        if (userService.modifyUser(userDto)) {
            return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
        }
        return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "회원 탈퇴", notes = "회원 탈퇴, DB 성공 여부에 따라 SUCCESS, FAIL 반환", response = String.class)
    @DeleteMapping("/{userNumber}")
    public ResponseEntity<String> userRemove(@PathVariable("userNumber") @ApiParam(value = "탈퇴할 회원번호") Long userNumber) throws Exception {
        logger.info("userRemove 호출 ");
        if (userService.removeUser(userNumber)) {
            return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
        }
        return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "회원 정보 전송", notes = "회원 정보 전송")
    @GetMapping("/info/{userNumber}")
    public ResponseEntity<UserPetDto> sendInfo(@PathVariable("userNumber") @ApiParam("전송할 회원번호") Long userNumber) throws Exception {
        logger.info("sendInfo 호출");
        UserPetDto userPetDto = userService.findUserInfo(userNumber);
        if (userPetDto != null) {
            return new ResponseEntity<UserPetDto>(userPetDto, HttpStatus.OK);
        }
        return new ResponseEntity<UserPetDto>(userPetDto, HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "회원 ID로 회원번호 조회")
    @GetMapping("/number/{userID}")
    public ResponseEntity<Long> sendNumber(@PathVariable("userID") @ApiParam("전송할 회원 ID") String userID) throws Exception {
        logger.info("sendNumber 호출");
        UserDto userDto = userService.findUserNumber(userID);
        Long userNumber = userDto.getUserNumber();
        return new ResponseEntity<Long>(userNumber, HttpStatus.OK);
    }

    @ApiOperation(value = "회원 팔로우하기", notes = "팔로우한 사람을 DB에 저장한다. 성공시 success, 실패시 fail 반환")
    @PostMapping("/follow/{fromUserNumber}/{toUserNumber}")
    public ResponseEntity<String> followAdd(@PathVariable(value = "fromUserNumber") @ApiParam("팔로우를 보내는 회원") Long fromUserNumber,
                                            @PathVariable(value = "toUserNumber") @ApiParam("팔로우를 당하는 회원") Long toUserNumber) throws Exception {
        logger.info("followAdd 호출");
        if (fromUserNumber.equals(toUserNumber)) return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
        if (userService.addFollow(fromUserNumber, toUserNumber)) {
            return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
        }
    }

    @ApiOperation(value = "회원 언팔로우하기", notes = "팔로우한 사람을 DB에 저장한다. 성공시 success, 실패시 fail 반환")
    @DeleteMapping("/follow/{fromUserNumber}/{toUserNumber}")
    public ResponseEntity<String> followRemove(@PathVariable(value = "fromUserNumber") @ApiParam("언팔로우를 보내는 회원") Long fromUserNumber,
                                               @PathVariable(value = "toUserNumber") @ApiParam("언팔로우를 당하는 회원") Long toUserNumber) throws Exception {
        logger.info("followRemove 호출");
        if (fromUserNumber.equals(toUserNumber)) return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
        if (userService.removeFollow(fromUserNumber, toUserNumber)) {
            return new ResponseEntity<String>(SUCCESS, HttpStatus.OK);
        } else {
            return new ResponseEntity<String>(FAIL, HttpStatus.NO_CONTENT);
        }
    }

    @ApiOperation(value = "팔로워 리스트 조회", notes = "나를 팔로우하고 있는 사람들의 목록 조회")
    @GetMapping("/follower/{userNumber}")
    public ResponseEntity<List<UserDto>> followerList(@PathVariable("userNumber") @ApiParam("팔로워 리스트를 조회할 회원번호") Long userNumber) throws Exception {
        logger.info("followerList 호출");
        List<UserDto> userDtoList = userService.findFollowList(userNumber);
        return new ResponseEntity<List<UserDto>>(userDtoList, HttpStatus.OK);
    }

    @ApiOperation(value = "팔로잉 리스트 조회", notes = "내가 팔로우 하고 있는 사람들의 목록 조회")
    @GetMapping("/following/{userNumber}")
    public ResponseEntity<List<UserDto>> followingList(@PathVariable("userNumber") @ApiParam("팔로워 리스트를 조회할 회원번호") Long userNumber) throws Exception {
        logger.info("followingList 호출");
        List<UserDto> userDtoList = userService.findFollowingList(userNumber);
        return new ResponseEntity<List<UserDto>>(userDtoList, HttpStatus.OK);
    }

    @ApiOperation(value = "회원가입 인증메일 보내기", notes = "메일을 보내고, 인증키를 반환한다.")
    @PostMapping("/emailConfirm/{userEmail}")
    public ResponseEntity<String> emailConfirm(@PathVariable("userEmail") @ApiParam(value = "이메일 주소") String userEmail) throws Exception {
        logger.info("sendEmail 호출");
        String authKey = emailService.sendSimpleMessage(userEmail);
        System.out.println(authKey);
        return new ResponseEntity<String>(authKey, HttpStatus.OK);
    }
}
