import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfigurableButtonComponent } from './configurable-button.component';
import { By } from '@angular/platform-browser';

describe('ConfigurableButtonComponent', () => {
  let component: ConfigurableButtonComponent;
  let fixture: ComponentFixture<ConfigurableButtonComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfigurableButtonComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigurableButtonComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  it('should render a button when type is "mat-flat-button"', () => {
    component.type = 'mat-flat-button';
    component.text = 'Click me';
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button'));
    expect(button).toBeTruthy();
    expect(button.nativeElement.textContent.trim()).toContain('Click me');
  });

  it('should render a span when type is "mat-icon-hover"', () => {
    component.type = 'mat-icon-hover';
    component.tooltip = 'Tooltip text';
    fixture.detectChanges();

    const span = fixture.debugElement.query(By.css('span'));
    expect(span).toBeTruthy();
  });

  it('should display the icon if provided', () => {
    component.icon = 'home';
    fixture.detectChanges();

    const icon = fixture.debugElement.query(By.css('mat-icon'));
    expect(icon).toBeTruthy();
    expect(icon.nativeElement.textContent.trim()).toBe('home');
  });

  it('should call handleClick when button is clicked', () => {
    spyOn(component, 'handleClick');
    component.type = 'mat-flat-button';
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button'));
    button.nativeElement.click();

    expect(component.handleClick).toHaveBeenCalled();
  });

  it('should execute the action when provided and button is clicked', () => {
    const actionSpy = jasmine.createSpy('actionSpy');
    component.type = 'mat-flat-button';
    component.action = actionSpy;
    fixture.detectChanges();

    const button = fixture.debugElement.query(By.css('button'));
    button.nativeElement.click();

    expect(actionSpy).toHaveBeenCalled();
  });
});
