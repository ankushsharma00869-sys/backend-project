package in.ankush.cloudshareapi.controller;

import in.ankush.cloudshareapi.document.UserCredits;
import in.ankush.cloudshareapi.dto.FileMetaDataDTO;
import in.ankush.cloudshareapi.service.FileMetaDataService;
import in.ankush.cloudshareapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private  final FileMetaDataService fileMetaDataService;
    private final UserCreditsService userCreditsService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestPart("files")MultipartFile files[]) throws IOException {
        Map<String, Object> response = new HashMap<>();
        List<FileMetaDataDTO> list = fileMetaDataService.uploadFiles(files);

       UserCredits finalCredits = userCreditsService.getUserCredits();

        response.put("files", list);
        response.put("remainingCredits", finalCredits.getCredits());
        return ResponseEntity.ok(response);
    }
     @GetMapping("my")
    public ResponseEntity<?> getFilesForCurrentUser(){
         List<FileMetaDataDTO> files = fileMetaDataService.getFiles();
         return ResponseEntity.ok(files);

    }
    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublicFile(@PathVariable String id){
        FileMetaDataDTO file = fileMetaDataService.getPublicFile(id);
        return  ResponseEntity.ok(file);

    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable String id) throws IOException {

        FileMetaDataDTO downloadableFile = fileMetaDataService.getDownloadableFile(id);
        Path path = Paths.get(downloadableFile.getFileLocation());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""+downloadableFile.getName()+"\"")
                .body(resource);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable String id){
        fileMetaDataService.deleteFile(id);
        return  ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<?> togglePublic(@PathVariable String id){
        FileMetaDataDTO file = fileMetaDataService.togglePublic(id);
        return ResponseEntity.ok(file);
    }
    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewFile(@PathVariable String id) throws IOException {

        FileMetaDataDTO file = fileMetaDataService.getPublicFile(id);

        Path path = Paths.get(file.getFileLocation());
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getType())) // 🔥 important
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"") // 🔥 inline = preview
                .body(resource);
    }


}
