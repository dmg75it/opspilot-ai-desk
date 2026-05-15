import { Ticket, TicketPriority, TicketStatus } from './ticket.model';

export interface DashboardStats {
  ticketsByStatus: Record<TicketStatus, number>;
  ticketsByPriority: Record<TicketPriority, number>;
  myOpenTickets: Ticket[];
  recentlyUpdated: Ticket[];
  aiInteractionsToday: number;
}
