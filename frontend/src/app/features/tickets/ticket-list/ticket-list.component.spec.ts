import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TicketListComponent } from './ticket-list.component';

describe('TicketListComponent', () => {
  let component: TicketListComponent;
  let fixture: ComponentFixture<TicketListComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketListComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(TicketListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tickets on init', () => {
    component.ngOnInit();
    http.expectOne(r => r.url.includes('/api/tickets')).flush({
      content: [
        { id: '1', title: 'Test', status: 'NEW', priority: 'HIGH', category: 'DELIVERY',
          createdByEmail: 'op@test.com', createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(), version: 0 }
      ],
      totalElements: 1, totalPages: 1, number: 0, size: 20
    });
    expect(component.tickets.length).toBe(1);
    expect(component.tickets[0].title).toBe('Test');
  });

  it('should show error when loading fails', () => {
    component.ngOnInit();
    http.expectOne(r => r.url.includes('/api/tickets')).flush('error', { status: 500, statusText: 'Error' });
    expect(component.error).toBeTruthy();
  });
});
