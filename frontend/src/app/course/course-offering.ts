// Matches the backend's CourseOfferingDto response.
export interface CourseOffering {
  readonly id: number;
  readonly courseId: number;
  readonly courseCode: string;
  readonly courseName: string;
  readonly academicYear: string;
  readonly period: string;
}
