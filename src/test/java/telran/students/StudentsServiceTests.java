package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.exeptions.NotFoundExeption;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.service.StudentsService;

@SpringBootTest
class StudentsServiceTests {
	@Autowired 
	StudentsService studentsService;
	@Autowired
	DbTestCreation dbCreation;
	@BeforeEach 
	void setUp() {
		dbCreation.createDb();
	}
	
	@Test
	@DisplayName("Testing method getMarks, normal flow")
	void getMarksTest() {
		Mark[] marksActual = studentsService.getMarks(1).toArray(Mark[]::new);
		Mark[] marksExpected = dbCreation.getStudentMarks(1);
		assertArrayEquals(marksExpected, marksActual);
	}
	@Test
	@DisplayName("Testing method getMarks, NotFoundException")
	void getMarksNotFoundTest() {
		assertThrowsExactly(NotFoundExeption.class, 
				() -> studentsService.getMarks(10));
	}
	
	@Test
	@DisplayName("Testing method addStudent, normal flow")
	void addStudentTest() {
		Student addedStudentActual = studentsService.addStudent(dbCreation.addedStudent);
		assertEquals(8, dbCreation.getAllStudents().size());
		Student addedStudentExpected = dbCreation.addedStudent;
		assertTrue(addedStudentExpected.equals(addedStudentActual));
	}
	@Test
	@DisplayName("Testing method addStudent, IllegalStateException, student already exists")
	void addExistStudentTest() {
		Student studentExist = dbCreation.students[0]; 
		assertThrowsExactly(IllegalStateException.class, 
				() -> studentsService.addStudent(studentExist));
	}
	
	@Test
	@DisplayName("Testing method updatePhone, normal flow")
	void updatePhoneTest() {
		Student studentUpdatedPhoneActual = studentsService.updatePhone(1l, dbCreation.PHONE_UPDATED);
		Student studentUpdatedPhoneExpected = dbCreation.studentUdatedPhone;
		assertTrue(studentUpdatedPhoneExpected.equals(studentUpdatedPhoneActual));
		assertTrue(studentUpdatedPhoneExpected
					.equals(dbCreation.getStudentUpdatedPhone(1l)));
	}
	@Test
	@DisplayName("Testing method updatePhone,  NotFoundExeption flow")
	void updatePhoneNotFoundStudentTest() {
		assertThrowsExactly(NotFoundExeption.class, 
				() -> studentsService.updatePhone(10l, dbCreation.PHONE_UPDATED));
	}
	
	@Test
	@DisplayName("Testing method addMark, normal flow")
	void addMarkTest() {
		List<Mark> marksActual = studentsService.addMark(7l, dbCreation.addedMark);
		List<Mark> marksExpected = dbCreation.marksWithAdded(7l, dbCreation.addedMark);
		assertTrue(marksExpected.equals(marksActual));
		List<Mark> addedMarks = dbCreation.getStudentMarksActual(7l);
		assertTrue(marksExpected.equals(addedMarks));
	}
	@Test
	@DisplayName("Testing method addMark, NotFoundException flow")
	void addMarkStudentNotFoundTest() {
		assertThrowsExactly(NotFoundExeption.class, 
				() -> studentsService.addMark(10l, dbCreation.addedMark));
	}
	
	@Test
	@DisplayName("Testing method removeStudent, normal flow")
	void removeStudentTest() {
		Student studentRemovedActual = studentsService.removeStudent(dbCreation.ID_1);
		Student studentRemovedExpected = dbCreation.studentRemoving;
		assertEquals(studentRemovedExpected, studentRemovedActual);
		List<Student> allStudentsActual = dbCreation.getAllStudents();
		assertEquals(6, allStudentsActual.size());
		
	}
	@Test
	@DisplayName("Testing method removeStudent, NotFoundException flow")
	void removeStudentNofoundTest() {
		assertThrowsExactly(NotFoundExeption.class, 
				() -> studentsService.removeStudent(10l));
	}


}
