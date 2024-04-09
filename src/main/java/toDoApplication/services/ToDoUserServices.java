package toDoApplication.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import toDoApplication.data.models.Task;
import toDoApplication.data.models.User;
import toDoApplication.data.repository.UserRepository;
import toDoApplication.dtos.requests.CompleteTaskRequest;
import toDoApplication.dtos.requests.DetailsRequest;
import toDoApplication.dtos.requests.RegisterRequest;
import toDoApplication.dtos.requests.TaskRequest;
import toDoApplication.dtos.response.CompleteTaskResponse;
import toDoApplication.dtos.response.ViewTaskResponse;
import toDoApplication.exception.TaskDoesNotExistException;
import toDoApplication.exception.UserNotFoundException;
import toDoApplication.exception.UsernameTakenException;
import toDoApplication.utils.Mappers;
import toDoApplication.utils.Validator;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static toDoApplication.data.models.TaskStatus.COMPLETED;
import static toDoApplication.data.models.TaskStatus.NOT_COMPLETED;
import static toDoApplication.utils.Mappers.mapToTask;
import static toDoApplication.utils.Validator.*;

@Service
public class ToDoUserServices implements UserService{
    public void save(User user){
        userRepository.save(user);
    }
    public long count(){
        return userRepository.count();
    }
    public void register(RegisterRequest request){
        validateRegisterRequest(request);
        if(isUsernameExisting(request.getUsername( )))
            throw new UsernameTakenException(request.getUsername( ));
        User user = Mappers.mapRequestToUser(request);
        save(user);
    }
    public void deleteUser(DetailsRequest detailsRequest){
        Validator.validateDetailsRequest(detailsRequest);
        List<User> users = userRepository.findAll();
        if(!users.isEmpty())
            for(User user: users){
                if(user.getUsername().equalsIgnoreCase(detailsRequest.getUsername()) &&
                user.getPassword().equalsIgnoreCase(detailsRequest.getPassword())){
                    userRepository.delete(user);
                    tasksServices.deleteUserTasks(detailsRequest.getUsername());
                    return;
                }
            }
        throw new UserNotFoundException();
    }
    public void createTask(TaskRequest taskRequest){
        Validator.validateTaskRequest(taskRequest);
        confirmUsername(taskRequest.getUsername());
        Task task = mapToTask(taskRequest);
        task.setStatus(NOT_COMPLETED);
        tasksServices.create(task);
    }
    public long countTasks(String username){
        if(!isUsernameExisting(username))
            throw new UserNotFoundException();
        return tasksServices.countUserTasks(username);
    }
    public void deleteAll(){
        userRepository.deleteAll();
        tasksServices.deleteAll();
    }
    public CompleteTaskResponse completeTask(CompleteTaskRequest completeTaskRequest){
        validateCompleteRequest(completeTaskRequest);
        if(isUsernameExisting(completeTaskRequest.getUsername())){
            return Mappers.mapCompleteTask(markTaskDone(completeTaskRequest););
        }
        throw new UserNotFoundException();
    }
    public boolean isTaskCompleted(CompleteTaskRequest completeTaskRequest){
        validateCompleteRequest(completeTaskRequest);
        confirmUsername(completeTaskRequest.getUsername());
        return tasksServices.findTask(completeTaskRequest).getStatus() == COMPLETED;
    }
    public ViewTaskResponse viewAllTasks(DetailsRequest request){
        validateDetailsRequest(request);
        if(!isExistingUser(request))
            throw new UserNotFoundException();
        confirmUsername(request.getUsername());
        ViewTaskResponse response = new ViewTaskResponse();
        response.setBody(getAllTasks(request.getUsername()));
        return response;
    }
    private String getAllTasks(String username){
        confirmUsername(username);
        StringBuilder output = new StringBuilder();
        for(Task task : tasksServices.findUserTasks(username)){
            output.append(String.format("Task Name : %s\nDue Date : %s\nStatus : %s\n",
                    task.getTaskName(),
                    task.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    task.getStatus()));
        }
        if( output.isEmpty( ))
            return "no tasks yet";
        return output.toString();
    }
    private void confirmUsername(String username){
        if(!isUsernameExisting(username))
            throw new UserNotFoundException();
    }
    private Task markTaskDone(CompleteTaskRequest request){
        for(Task task : tasksServices.findAll( )){
            if(task.getTaskUser().equalsIgnoreCase(request.getUsername())){
                if( task.getTaskName( ).equalsIgnoreCase(request.getTaskName( )) ){
                    task.setStatus(COMPLETED);
                    tasksServices.save(task);
                    return task;
                }
            }
        }
        throw new TaskDoesNotExistException();
    }
    private boolean isUsernameExisting(String username){
      Optional<User> user= userRepository.findByUsername(username);
      return user.isPresent();
    }
    private boolean isExistingUser(DetailsRequest request){
       Optional<User> user = userRepository.findByUsername(request.getUsername( ));
        return user.isPresent( )&&user.get( ).getPassword( ).equalsIgnoreCase(request.getPassword( ));
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TasksServices tasksServices;
}
