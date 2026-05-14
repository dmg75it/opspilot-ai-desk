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

export interface TicketSummary {
  id: string;
  externalRef?: string;
  title: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  assignedToName?: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface UserRef {
  id: string;
  email: string;
  fullName: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_FOR_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory = 'DELIVERY' | 'PICKUP' | 'DOCUMENTATION' | 'CUSTOMER' | 'SYSTEM' | 'OTHER';

export interface TicketCreateRequest {
  externalRef?: string;
  title: string;
  description: string;
  priority: TicketPriority;
  category: TicketCategory;
}

export interface TicketUpdateRequest {
  title?: string;
  description?: string;
  priority?: TicketPriority;
  category?: TicketCategory;
  externalRef?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface TicketNote {
  id: string;
  ticketId: string;
  authorName: string;
  body: string;
  visibility: 'INTERNAL' | 'AI_SUMMARY' | 'SYSTEM';
  createdAt: string;
}
