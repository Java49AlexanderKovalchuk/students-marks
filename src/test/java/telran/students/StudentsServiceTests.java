package telran.students;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import telran.exceptions.NotFoundException;
import telran.students.dto.Mark;
import telran.students.dto.Student;
import telran.students.repo.StudentRepo;
import telran.students.service.StudentsService;
@SpringBootTest
class StudentsServiceTests {
	@Autowired
StudentsService studentsService;
	@Autowired
	DbTestCreation dbCreation;
	@Autowired
StudentRepo studentRepo;	
	@BeforeEach
	void setUp() {
		dbCreation.createDB();
	}
	@Test
	void getMarksTest() {
		Mark[] marksActual = studentsService.getMarks(1).toArray(Mark[]::new);
		Mark[] marksExpected = dbCreation.getStudentMarks(1);
		assertArrayEquals(marksExpected, marksActual);
		assertThrowsExactly(NotFoundException.class, () -> studentsService.getMarks(10000000));
	}
	@Test
	void removeStudentTest() {
		assertEquals(dbCreation.getStudent(1), studentsService.removeStudent(1));
		assertThrowsExactly(NotFoundException.class, ()->studentsService.removeStudent(1));
	}
	@Test
	void addMarkTest() {
		List<Mark> marksStudent2Expected = new ArrayList<>(Arrays.asList(dbCreation.getStudentMarks(2)));
		Mark mark = new Mark("Java", LocalDate.now(), 100);
		marksStudent2Expected.add(mark);
		assertIterableEquals(marksStudent2Expected, studentsService.addMark(2, mark));
		assertIterableEquals(marksStudent2Expected, studentsService.getMarks(2));
		assertThrowsExactly(NotFoundException.class, () -> studentsService.addMark(0, mark));
	}

	@Test
	void updatePhoneTest() {
		Student student1 = dbCreation.getStudent(3);
		String newPhone = "055-5555555";
		Student expected = new Student(student1.id(), student1.name(), newPhone);
		assertEquals(expected, studentsService.updatePhone(3, newPhone));
		Student actual = studentRepo.findById(3l).orElseThrow().build();
		assertEquals(expected, actual);
		assertThrowsExactly(NotFoundException.class, () -> studentsService.updatePhone(0, newPhone));
	}
	@Test
	void addStudentTest() {
		Student studentExisting = dbCreation.getStudent(4);
		Student newStudent = new Student(-1l, "Vasya", "111111111");
		assertEquals(newStudent, studentsService.addStudent(newStudent));
		Student actual = studentRepo.findById(-1l).orElseThrow().build();
		assertEquals(newStudent, actual);
		assertThrowsExactly(IllegalStateException.class,
				()-> studentsService.addStudent(studentExisting));
	}


}