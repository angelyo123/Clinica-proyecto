import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChatIaPaciente } from './chat-ia-paciente';

describe('ChatIaPaciente', () => {
  let component: ChatIaPaciente;
  let fixture: ComponentFixture<ChatIaPaciente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChatIaPaciente]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChatIaPaciente);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
