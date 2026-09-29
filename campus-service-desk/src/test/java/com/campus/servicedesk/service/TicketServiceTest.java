package com.campus.servicedesk.service;

import com.campus.servicedesk.dto.*;
import com.campus.servicedesk.entity.*;
import com.campus.servicedesk.exception.BadRequestException;
import com.campus.servicedesk.exception.ForbiddenException;
import com.campus.servicedesk.exception.ResourceNotFoundException;
import com.campus.servicedesk.repository.TicketCommentRepository;
import com.campus.servicedesk.repository.TicketRepository;
import com.campus.servicedesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCommentRepository commentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TicketService ticketService;

    private User student;
    private User otherStudent;
    private User agent;
    private User admin;
    private Ticket sampleTicket;

    @BeforeEach
    void setUp() {
        student = User.builder().id(1L).email("student@u.edu").fullName("Student One").role(Role.STUDENT).build();
        otherStudent = User.builder().id(2L).email("other@u.edu").fullName("Student Two").role(Role.STUDENT).build();
        agent = User.builder().id(3L).email("agent@u.edu").fullName("Agent One").role(Role.SUPPORT_AGENT).build();
        admin = User.builder().id(4L).email("admin@u.edu").fullName("Admin One").role(Role.ADMIN).build();

        sampleTicket = Ticket.builder()
                .id(10L)
                .title("Cannot access Wi-Fi")
                .description("Wi-Fi not working in building A")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.MEDIUM)
                .creator(student)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createTicket()")
    class CreateTicket {

        @Test
        @DisplayName("should create a ticket for student")
        void createSuccess() {
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .title("Printer broken")
                    .description("Printer in lab 201 jammed")
                    .build();

            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
                Ticket t = inv.getArgument(0);
                t.setId(11L);
                t.setCreatedAt(LocalDateTime.now());
                t.setUpdatedAt(LocalDateTime.now());
                return t;
            });

            TicketResponse response = ticketService.createTicket(request, student);

            assertThat(response.getId()).isEqualTo(11L);
            assertThat(response.getStatus()).isEqualTo("OPEN");
            assertThat(response.getPriority()).isEqualTo("MEDIUM");
            assertThat(response.getCreatorId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should reject invalid priority")
        void rejectInvalidPriority() {
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .title("Test")
                    .description("Test desc")
                    .priority("SUPER_HIGH")
                    .build();

            assertThatThrownBy(() -> ticketService.createTicket(request, student))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid priority");
        }
    }

    @Nested
    @DisplayName("getTicketById() — access control")
    class GetTicket {

        @Test
        @DisplayName("student can view their own ticket")
        void studentViewsOwnTicket() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

            TicketResponse response = ticketService.getTicketById(10L, student);
            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("student CANNOT view another student's ticket")
        void studentCannotViewOtherTicket() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

            assertThatThrownBy(() -> ticketService.getTicketById(10L, otherStudent))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("your own tickets");
        }

        @Test
        @DisplayName("agent can view assigned ticket")
        void agentViewsAssignedTicket() {
            sampleTicket.setAssignedAgent(agent);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

            TicketResponse response = ticketService.getTicketById(10L, agent);
            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("agent CANNOT view unassigned ticket")
        void agentCannotViewUnassignedTicket() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

            assertThatThrownBy(() -> ticketService.getTicketById(10L, agent))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("assigned to you");
        }

        @Test
        @DisplayName("admin can view any ticket")
        void adminViewsAnyTicket() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(commentRepository.findByTicketIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());

            TicketResponse response = ticketService.getTicketById(10L, admin);
            assertThat(response.getId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("should throw 404 for non-existent ticket")
        void ticketNotFound() {
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketById(999L, student))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("assignTicket()")
    class AssignTicket {

        @Test
        @DisplayName("should assign ticket to agent and set IN_PROGRESS")
        void assignSuccess() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(userRepository.findById(3L)).thenReturn(Optional.of(agent));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            AssignTicketRequest request = AssignTicketRequest.builder().agentId(3L).build();
            TicketResponse response = ticketService.assignTicket(10L, request);

            assertThat(response.getAssignedAgentId()).isEqualTo(3L);
            assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        }

        @Test
        @DisplayName("should reject assigning to a non-agent user")
        void rejectNonAgent() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(userRepository.findById(1L)).thenReturn(Optional.of(student));

            AssignTicketRequest request = AssignTicketRequest.builder().agentId(1L).build();

            assertThatThrownBy(() -> ticketService.assignTicket(10L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("not a support agent");
        }
    }

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("assigned agent can update status")
        void agentUpdatesStatus() {
            sampleTicket.setAssignedAgent(agent);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateStatusRequest request = UpdateStatusRequest.builder().status("RESOLVED").build();
            TicketResponse response = ticketService.updateStatus(10L, request, agent);

            assertThat(response.getStatus()).isEqualTo("RESOLVED");
        }

        @Test
        @DisplayName("unassigned agent CANNOT update status")
        void unassignedAgentCannotUpdate() {
            User otherAgent = User.builder().id(5L).role(Role.SUPPORT_AGENT).build();
            sampleTicket.setAssignedAgent(agent);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

            UpdateStatusRequest request = UpdateStatusRequest.builder().status("RESOLVED").build();

            assertThatThrownBy(() -> ticketService.updateStatus(10L, request, otherAgent))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("admin can update any ticket status")
        void adminUpdatesAnyStatus() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateStatusRequest request = UpdateStatusRequest.builder().status("CLOSED").build();
            TicketResponse response = ticketService.updateStatus(10L, request, admin);

            assertThat(response.getStatus()).isEqualTo("CLOSED");
        }
    }

    @Nested
    @DisplayName("addComment()")
    class AddComment {

        @Test
        @DisplayName("ticket creator can comment")
        void creatorCanComment() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));
            when(commentRepository.save(any(TicketComment.class))).thenAnswer(inv -> {
                TicketComment c = inv.getArgument(0);
                c.setId(1L);
                c.setCreatedAt(LocalDateTime.now());
                return c;
            });

            CreateCommentRequest request = CreateCommentRequest.builder().content("Please help!").build();
            CommentResponse response = ticketService.addComment(10L, request, student);

            assertThat(response.getContent()).isEqualTo("Please help!");
        }

        @Test
        @DisplayName("unrelated student CANNOT comment")
        void unrelatedStudentCannotComment() {
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(sampleTicket));

            CreateCommentRequest request = CreateCommentRequest.builder().content("Hijack!").build();

            assertThatThrownBy(() -> ticketService.addComment(10L, request, otherStudent))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("listTickets()")
    class ListTickets {

        @Test
        @DisplayName("student only sees own tickets")
        void studentListsOwnTickets() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Ticket> page = new PageImpl<>(List.of(sampleTicket));
            when(ticketRepository.findByCreatorId(1L, pageable)).thenReturn(page);

            Page<TicketResponse> result = ticketService.listTickets(student, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(ticketRepository).findByCreatorId(1L, pageable);
        }

        @Test
        @DisplayName("agent sees assigned tickets")
        void agentListsAssignedTickets() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Ticket> page = new PageImpl<>(List.of());
            when(ticketRepository.findByAssignedAgentId(3L, pageable)).thenReturn(page);

            Page<TicketResponse> result = ticketService.listTickets(agent, null, pageable);

            verify(ticketRepository).findByAssignedAgentId(3L, pageable);
        }

        @Test
        @DisplayName("admin sees all tickets")
        void adminListsAllTickets() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Ticket> page = new PageImpl<>(List.of(sampleTicket));
            when(ticketRepository.findAll(pageable)).thenReturn(page);

            Page<TicketResponse> result = ticketService.listTickets(admin, null, pageable);

            verify(ticketRepository).findAll(pageable);
        }
    }
}
