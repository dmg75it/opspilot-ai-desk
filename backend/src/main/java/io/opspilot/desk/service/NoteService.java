package io.opspilot.desk.service;

import io.opspilot.desk.dto.note.*;
import io.opspilot.desk.entity.*;
import io.opspilot.desk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final TicketNoteRepository noteRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional
    public NoteResponse addNote(UUID ticketId, CreateNoteRequest req, String authorEmail) {
        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
        var author = userRepository.findByEmail(authorEmail).orElseThrow();
        var note = new TicketNote();
        note.setTicket(ticket);
        note.setAuthor(author);
        note.setBody(req.body());
        note.setVisibility(TicketNote.Visibility.INTERNAL);
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void addSystemNote(Ticket ticket, String body) {
        var note = new TicketNote();
        note.setTicket(ticket);
        note.setBody(body);
        note.setVisibility(TicketNote.Visibility.SYSTEM);
        noteRepository.save(note);
    }

    @Transactional
    public NoteResponse addAiSummaryNote(Ticket ticket, String body) {
        var note = new TicketNote();
        note.setTicket(ticket);
        note.setBody(body);
        note.setVisibility(TicketNote.Visibility.AI_SUMMARY);
        return toResponse(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listNotes(UUID ticketId) {
        var ticket = ticketRepository.findById(ticketId)
            .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return noteRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
            .map(this::toResponse).toList();
    }

    private NoteResponse toResponse(TicketNote n) {
        return new NoteResponse(n.getId(),
            n.getTicket().getId(),
            n.getAuthor() != null ? n.getAuthor().getEmail() : null,
            n.getBody(), n.getVisibility().name(), n.getCreatedAt());
    }
}
