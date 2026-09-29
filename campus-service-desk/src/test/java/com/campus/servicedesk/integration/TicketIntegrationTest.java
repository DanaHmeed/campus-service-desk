package com.campus.servicedesk.integration;

import com.campus.servicedesk.dto.*;
import com.campus.servicedesk.repository.TicketCommentRepository;
import com.campus.servicedesk.repository.TicketRepository;
import com.campus.servicedesk.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private TicketCommentRepository commentRepository;

    private String studentToken;
    private String otherStudentToken;
    private String agentToken;
    private String adminToken;
    private Long agentId;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        userRepository.deleteAll();

        studentToken = registerAndGetToken("student@u.edu", "Student One", "password123", "STUDENT");
        otherStudentToken = registerAndGetToken("other@u.edu", "Student Two", "password123", "STUDENT");
        agentToken = registerAndGetToken("agent@u.edu", "Agent One", "password123", "SUPPORT_AGENT");
        AuthResponse adminAuth = registerAndGetAuth("admin@u.edu", "Admin One", "password123", "ADMIN");
        adminToken = adminAuth.getToken();

        // Get agent ID for assignment
        AuthResponse agentAuth = objectMapper.readValue(
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        LoginRequest.builder().email("agent@u.edu").password("password123").build())))
                        .andReturn().getResponse().getContentAsString(),
                AuthResponse.class);
        agentId = agentAuth.getUserId();
    }

    // ── Ticket Creation ──────────────────────────────────────────────────

    @Test
    @DisplayName("STUDENT can create a ticket")
    void studentCreatesTicket() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Cannot access Wi-Fi")
                .description("Wi-Fi not working in building A")
                .priority("HIGH")
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Cannot access Wi-Fi"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @DisplayName("AGENT cannot create a ticket (403)")
    void agentCannotCreateTicket() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Test")
                .description("Test description")
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ticket creation rejects blank title")
    void rejectBlankTitle() throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("")
                .description("Some description")
                .build();

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    // ── Ticket Access Control ────────────────────────────────────────────

    @Test
    @DisplayName("Student CANNOT view another student's ticket (403)")
    void studentCannotViewOtherStudentTicket() throws Exception {
        // Student 1 creates a ticket
        Long ticketId = createTicketAndGetId(studentToken, "My private ticket", "Details");

        // Student 2 tries to view it
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Student can view their own ticket")
    void studentCanViewOwnTicket() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "My ticket", "Details");

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My ticket"));
    }

    @Test
    @DisplayName("ADMIN can view any ticket")
    void adminCanViewAnyTicket() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "Student ticket", "Details");

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Student ticket"));
    }

    // ── Ticket Assignment ────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN can assign ticket to an agent")
    void adminAssignsTicket() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "Needs assignment", "Details");

        AssignTicketRequest assign = AssignTicketRequest.builder().agentId(agentId).build();

        mockMvc.perform(patch("/api/admin/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAgentId").value(agentId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("STUDENT cannot access admin endpoints (403)")
    void studentCannotAssignTicket() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "Student tries admin", "Details");

        AssignTicketRequest assign = AssignTicketRequest.builder().agentId(agentId).build();

        mockMvc.perform(patch("/api/admin/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(status().isForbidden());
    }

    // ── Status Updates ───────────────────────────────────────────────────

    @Test
    @DisplayName("Assigned AGENT can update ticket status")
    void assignedAgentUpdatesStatus() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "To be resolved", "Details");

        // Admin assigns
        AssignTicketRequest assign = AssignTicketRequest.builder().agentId(agentId).build();
        mockMvc.perform(patch("/api/admin/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(status().isOk());

        // Agent resolves
        UpdateStatusRequest statusReq = UpdateStatusRequest.builder().status("RESOLVED").build();
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    // ── Comments ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Student can comment on own ticket")
    void studentCommentsOnOwnTicket() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "Commentable", "Details");

        CreateCommentRequest comment = CreateCommentRequest.builder().content("Please hurry!").build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Please hurry!"));
    }

    @Test
    @DisplayName("Other student CANNOT comment on ticket (403)")
    void otherStudentCannotComment() throws Exception {
        Long ticketId = createTicketAndGetId(studentToken, "Not yours", "Details");

        CreateCommentRequest comment = CreateCommentRequest.builder().content("Hacking!").build();

        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comment)))
                .andExpect(status().isForbidden());
    }

    // ── Statistics ───────────────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN can view ticket statistics")
    void adminViewsStats() throws Exception {
        createTicketAndGetId(studentToken, "Ticket 1", "D1");
        createTicketAndGetId(studentToken, "Ticket 2", "D2");

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(2))
                .andExpect(jsonPath("$.ticketsByStatus.OPEN").value(2));
    }

    // ── Full Workflow ────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: create → assign → comment → resolve → close")
    void fullTicketLifecycle() throws Exception {
        // 1. Student creates ticket
        Long ticketId = createTicketAndGetId(studentToken, "Full lifecycle test", "Printer broken");

        // 2. Admin assigns to agent
        AssignTicketRequest assign = AssignTicketRequest.builder().agentId(agentId).build();
        mockMvc.perform(patch("/api/admin/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assign)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // 3. Agent comments
        CreateCommentRequest agentComment = CreateCommentRequest.builder().content("Looking into it").build();
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentComment)))
                .andExpect(status().isCreated());

        // 4. Student comments back
        CreateCommentRequest studentComment = CreateCommentRequest.builder().content("Thanks!").build();
        mockMvc.perform(post("/api/tickets/" + ticketId + "/comments")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentComment)))
                .andExpect(status().isCreated());

        // 5. Agent resolves
        UpdateStatusRequest resolve = UpdateStatusRequest.builder().status("RESOLVED").build();
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolve)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 6. Admin closes
        UpdateStatusRequest close = UpdateStatusRequest.builder().status("CLOSED").build();
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(close)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        // 7. Verify detail view includes comments
        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments", hasSize(2)))
                .andExpect(jsonPath("$.comments[0].content").value("Looking into it"))
                .andExpect(jsonPath("$.comments[1].content").value("Thanks!"));
    }

    // ── Pagination ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Ticket list supports pagination")
    void ticketPagination() throws Exception {
        // Create 3 tickets
        for (int i = 1; i <= 3; i++) {
            createTicketAndGetId(studentToken, "Ticket " + i, "Description " + i);
        }

        // Page size 2
        mockMvc.perform(get("/api/tickets?page=0&size=2")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Page 2
        mockMvc.perform(get("/api/tickets?page=1&size=2")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private String registerAndGetToken(String email, String name, String password, String role) throws Exception {
        AuthResponse auth = registerAndGetAuth(email, name, password, role);
        return auth.getToken();
    }

    private AuthResponse registerAndGetAuth(String email, String name, String password, String role) throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .email(email).fullName(name).password(password).role(role).build();

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private Long createTicketAndGetId(String token, String title, String description) throws Exception {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title(title).description(description).build();

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TicketResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TicketResponse.class);
        return response.getId();
    }
}
