package com.example.final_project_java.car.service;

import com.example.final_project_java.car.dto.request.RentCarRequestDTO;
import com.example.final_project_java.car.dto.request.RentCarResModifyRequestDTO;
import com.example.final_project_java.car.dto.response.RentCarDetailResponseDTO;
import com.example.final_project_java.car.dto.response.RentCarListResponseDTO;
import com.example.final_project_java.car.entity.Car;
import com.example.final_project_java.car.entity.RentCar;
import com.example.final_project_java.car.repository.CarRepository;
import com.example.final_project_java.car.repository.RentCarRepository;
import com.example.final_project_java.userapi.entity.Role;
import com.example.final_project_java.userapi.entity.User;
import com.example.final_project_java.userapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class RentCarService {

   private final CarRepository carRepository;
   private final UserRepository userRepository;
   private final RentCarRepository rentCarRepository;


   // 전기차 예약 목록 보기 (관리자가 모든 예약 보는것)
   public RentCarListResponseDTO getList() {
      List<RentCar> entityList = rentCarRepository.findAll();

      List<RentCarDetailResponseDTO> dtoList = entityList.stream()
              .map(RentCarDetailResponseDTO::new)
              .collect(Collectors.toList());

      return RentCarListResponseDTO.builder()
              .rentList(dtoList)
              .build();
   }


   // 사용자 Role 통해서 정보 불러오기
//   private Optional<User> getUserRole(String userId) {
//
//      Optional<User> user = rentCarRepository.findUserByUserIdOnly(userId);
//
//      return user;
//   }


   // 한 유저의 전기차 예약 목록을 가져온다.
   public RentCarListResponseDTO getRentListByUser(String userId) {

      List<RentCar> entityList = rentCarRepository.findByUserId(userId);
      List<RentCarDetailResponseDTO> dtoList = entityList.stream()
              .map(RentCarDetailResponseDTO::new)
              .collect(Collectors.toList());

      return RentCarListResponseDTO.builder()
              .rentList(dtoList)
              .build();
   }


   // 전기차 예약 상세보기 가져오기
   public RentCarListResponseDTO reservationInfo (int carNo) {

      RentCar reservation = getReservation(carNo);

      RentCarDetailResponseDTO responseDTO = new RentCarDetailResponseDTO(reservation);

      List<RentCarDetailResponseDTO> dtoList = Collections.singletonList(responseDTO);

      return RentCarListResponseDTO.builder()
              .rentList(dtoList)
              .build();
   }

   // 전기차 예약PK로 상세 정보 가져오기
   private RentCar getReservation(int carNo) {
      return rentCarRepository.findById(carNo).orElseThrow(
              () -> new RuntimeException("예약내역이 없습니다." + carNo)
      );
   }

   // 예약하기
   public RentCarDetailResponseDTO reservation (
           final RentCarRequestDTO requestDTO,
           final String email,
           final String carId,
           final String carName,
           final LocalDate rentDate,
           final LocalDate turninDate,
           final Time rentTime,
           final Time turninTime
           ) {
      User user = getUser(email);
      Car carInfo = getCarInfo(carId);
      log.info("carInfo - {}", carInfo);

      // 예외처리
      if (rentCarRepository.existsByCarIdAndRentDateBetween(carId, rentDate, turninDate)){
         throw new IllegalStateException("이미 예약하신 차가 있습니다.");
      }

      RentCar save = rentCarRepository.save(requestDTO.toEntity(user, carInfo));
      log.info("차량 예약 완료.");

      return null;
   }

   // 예약 상세
   private Car getCarInfo(String carId) {
      Car car = carRepository.findById(carId).orElseThrow(
              () -> new RuntimeException("차량정보가 존재하지 않습니다.")
      );
      return car;
   }

   // 유저
   private User getUser(String email) {
      User user = userRepository.findByEmail(email).orElseThrow(
              () -> new RuntimeException("회원정보가 존재하지 않습니다.")
      );
      return user;
   }


   // 렌트카 예약 삭제 (유저가 본인 예약 삭제하는것)
//   public RentCarListResponseDTO delete(int carNo, String userId) {
//      RentCar deleteReservation = rentCarRepository.findById(carNo).orElseThrow(
//              () -> {
//                 log.error("예약번호가 조회되지 않아 삭제가 불가능합니다. 예약순서: {}", carNo);
//                 throw new RuntimeException("예약번호가 존재하지 않아 삭제에 실패했습니다.");
//              }
//      );
//      rentCarRepository.deleteById(carNo);
//
//      return getRentListByUser(userId);
//   }


   // 렌트카 예약 삭제
   public RentCarListResponseDTO delete(int carNo, String email) {
      User user = getUser(email);


      RentCar deleteReservation = rentCarRepository.findById(carNo).orElseThrow(
              () -> {
                 log.error("예약번호가 조회되지 않아 삭제가 불가능합니다. 예약순서: {}", carNo);
                 throw new RuntimeException("예약번호가 존재하지 않아 삭제에 실패했습니다.");
              }
      );
      rentCarRepository.deleteById(carNo);

      return getList();
   }

   // 렌트카 예약 수정
   public RentCarListResponseDTO update(RentCarResModifyRequestDTO requestDTO, int carNo, String email) {
      User user = getUser(email);

      // 회원인지 확인
      if (user.getRole() != Role.COMMON) {
         throw new IllegalArgumentException("해당 사용자는 수정 권한이 없습니다.");
      }

      Optional<RentCar> targetEntity = rentCarRepository.findById(carNo);


      if (targetEntity.isPresent()) {
         RentCar reservation = targetEntity.get();
         reservation.setRentTime(requestDTO.getRentTime()); // 픽업시간 설정
         reservation.setTurninTime(requestDTO.getTurninTime()); // 반납 시간 설정
         reservation.setExtra(requestDTO.getExtra()); // 비고 설정
         RentCar savedReservation = rentCarRepository.save(reservation);

         // RentCarDetailResponseDTO 객체를 생성
         RentCarDetailResponseDTO detailResponseDTO = new RentCarDetailResponseDTO(savedReservation);

         // RentCarListResponseDTO 객체 생성하고 rentList에 detailResponseDTO를 포함.
         List<RentCarDetailResponseDTO> rentList = Collections.singletonList(detailResponseDTO);
         RentCarListResponseDTO responseDTO = RentCarListResponseDTO.builder()
                 .rentList(rentList)
                 .build();

         // 응답 DTO를 반환
         return responseDTO;
      } else {
         // 예약 정보를 찾을 수 없을 때 예외를 발생
         throw new IllegalArgumentException("예약 정보를 찾을 수 없습니다." + carNo);
      }

   }

   // 달력에 예약한 날짜들 표시하기 (예약 못하게)
   public List<LocalDate> searchDate (String carId ) {
      Car byCarId = rentCarRepository.findByCarId(carId);

      if (carId == null) {
         throw new RuntimeException("해당 ID의 차량을 찾을 수 없습니다.");
      }
      List<LocalDate> reservationDates = rentCarRepository.findReservedDatesByCarId(carId);

      return rentCarRepository.findReservedDatesByCarId(carId);
   }

}












































