export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_FOR_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory = 'DELIVERY' | 'PICKUP' | 'DOCUMENTATION' | 'CUSTOMER' | 'SYSTEM' | 'OTHER';

export interface Ticket {
  id: string;
  externalRef?: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  assignedToEmail?: string;
  createdByEmail: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  version: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: string;
  category: string;
  externalRef?: string;
}

export interface ChangeStatusRequest {
  status: string;
}

export interface Note {
  id: string;
  ticketId: string;
  authorEmail?: string;
  body: string;
  visibility: 'INTERNAL' | 'AI_SUMMARY' | 'SYSTEM';
  createdAt: string;
}
