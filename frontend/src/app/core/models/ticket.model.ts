export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_FOR_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory = 'DELIVERY' | 'PICKUP' | 'DOCUMENTATION' | 'CUSTOMER' | 'SYSTEM' | 'OTHER';
export type NoteVisibility = 'INTERNAL' | 'AI_SUMMARY' | 'SYSTEM';

export interface UserRef {
  id: string;
  email: string;
  fullName: string;
  role: string;
}

export interface Ticket {
  id: string;
  externalRef?: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  assignedTo?: UserRef;
  createdBy: UserRef;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  version: number;
}

export interface TicketPage {
  content: Ticket[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface TicketNote {
  id: string;
  ticketId: string;
  author: UserRef;
  body: string;
  visibility: NoteVisibility;
  createdAt: string;
}

export interface CreateTicketRequest {
  externalRef?: string;
  title: string;
  description: string;
  priority: TicketPriority;
  category: TicketCategory;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: TicketPriority;
  category?: TicketCategory;
  externalRef?: string;
}
