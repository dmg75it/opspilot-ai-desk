import { Ticket } from './ticket.model';

export interface DashboardData {
  ticketsByStatus: { [status: string]: number };
  ticketsByPriority: { [priority: string]: number };
  myOpenTickets: Ticket[];
  recentTickets: Ticket[];
  aiInteractionsToday: number;
}
