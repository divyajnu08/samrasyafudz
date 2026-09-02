package in.samrasyafudz.productservice.controller;

import in.samrasyafudz.productservice.service.CategoryService;
import in.samrasyafudz.productservice.service.ImageStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final ImageStorageService imageStorageService;
    private final CategoryService categoryService;

    public AdminCategoryController(ImageStorageService imageStorageService, CategoryService categoryService) {
        this.imageStorageService = imageStorageService;
        this.categoryService = categoryService;
    }

    @PostMapping("/{categoryId}/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file) throws IOException {

        String imageUrl = imageStorageService.uploadProductImage(categoryId, file);

        categoryService.updateImageUrl(categoryId, imageUrl);

        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

}
