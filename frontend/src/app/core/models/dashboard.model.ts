import { Ticket } from './ticket.model';

export interface DashboardData {
  ticketsByStatus: Record<string, number>;
  ticketsByPriority: Record<string, number>;
  myOpenTickets: Ticket[];
  recentlyUpdated: Ticket[];
  aiInteractionsToday: number;
}
