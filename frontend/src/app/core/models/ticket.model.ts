export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_FOR_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory = 'DELIVERY' | 'PICKUP' | 'DOCUMENTATION' | 'CUSTOMER' | 'SYSTEM' | 'OTHER';

export interface Ticket {
  id: number;
  externalRef?: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  assignedToId?: number;
  assignedToName?: string;
  createdById: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  version: number;
}

export interface TicketNote {
  id: number;
  ticketId: number;
  authorId: number;
  authorName: string;
  body: string;
  visibility: 'INTERNAL' | 'AI_SUMMARY' | 'SYSTEM';
  createdAt: string;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: TicketPriority;
  category: TicketCategory;
  externalRef?: string;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: TicketPriority;
  category?: TicketCategory;
  externalRef?: string;
  version: number;
}

export interface ChangeStatusRequest {
  status: TicketStatus;
  reason?: string;
}

export interface AssignTicketRequest {
  operatorId: number;
}
