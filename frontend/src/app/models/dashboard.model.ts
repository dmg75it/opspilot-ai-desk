import { TicketSummary } from './ticket.model';

export interface DashboardData {
  ticketsByStatus: Record<string, number>;
  ticketsByPriority: Record<string, number>;
  myOpenTickets: TicketSummary[];
  recentlyUpdated: TicketSummary[];
  aiInteractionsToday: number;
}
