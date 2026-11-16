import { Component } from '@angular/core';
import { ReportesDashboardComponent } from '@siga/academico';

@Component({
  selector: 'siga-admin-reportes',
  imports: [ReportesDashboardComponent],
  template: `<siga-reportes-dashboard />`,
})
export class AdminReportesComponent {}
