package toDoApplication.dtos.requests;

import lombok.Data;

@Data
public class TaskRequest{
    private String taskName;
    private String dueDate;
}
